package com.retronova.engine.sound;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import kuusisto.tinysound.Music;
import kuusisto.tinysound.TinySound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sound {
	
	private static Map<String, kuusisto.tinysound.Sound> sounds;
	/** As faixas já carregadas. Enche por uma thread de fundo; ver load. */
	private static final Map<String, Music> musics = new java.util.concurrent.ConcurrentHashMap<>();
	/** Faixas pedidas antes de chegarem: tocam no Sound.tick em que ficarem prontas. */
	private static final Map<Musics, Boolean> pendentes = new java.util.EnumMap<>(Musics.class);
	private static volatile boolean musicasProntas;
	/** O que a audicao das opcoes pausou. Ver pausarParaAudicao. */
	private static final java.util.List<Musics> emAudicao = new java.util.ArrayList<>();

	/**
	 * Carrega os efeitos agora e as músicas por trás.
	 *
	 * AS MÚSICAS EM SEGUNDO PLANO. A TinySound decodifica cada OGG inteiro ao
	 * carregar: os 10 MB de trilha levavam 3,4 s, e com os efeitos o jogo ficava
	 * 3,9 s sem janela nenhuma depois do clique. Os efeitos (0,2 s) continuam aqui,
	 * porque a abertura precisa deles; as músicas carregam numa thread enquanto a
	 * abertura roda, e a da tela inicial primeiro. Pedir uma faixa que ainda não
	 * chegou não quebra nada: ela fica pendente e começa quando estiver pronta.
	 */
	public static synchronized void load() {
		TinySound.init();
        if(!TinySound.isInitialized())
			throw new RuntimeException("Error on load sounds!");
		TinySound.setGlobalVolume(1f);
		if(sounds == null)
			loadSounds();
		if(musics.isEmpty() && !musicasProntas) {
			Thread t = new Thread(Sound::loadMusics, "Sound-musicas");
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		}
	}

	/** Todas as músicas já carregaram. */
	public static boolean musicasProntas() {
		return musicasProntas;
	}

	/** Esta faixa já carregou e pode tocar na hora. */
	public static boolean pronta(Musics music) {
		return musics.containsKey(music.resource());
	}

	/**
	 * Efeitos ficam em WAV e em memória.
	 *
	 * São curtos e disparam a todo momento: descomprimir a cada disparo
	 * introduziria latência justo onde ela é mais perceptível. Somados, ocupam
	 * pouco mais de 9 MB, então o custo em disco não compensa comprimir.
	 */
	private static synchronized void loadSounds() {
		sounds = new HashMap<>();
		Sounds[] names = Sounds.values();
		for(Sounds name : names) {
			String caminho = Engine.resPath + "audio/" + name.resource() + ".wav";
			kuusisto.tinysound.Sound sound = TinySound.loadSound(caminho);
			sounds.put(name.resource(), sound);
			duracoes.put(name, duracaoEmMs(caminho));
		}
	}

	/** Duração de cada efeito, para o limite de vozes saber quando uma termina. */
	private static final Map<Sounds, Long> duracoes = new java.util.EnumMap<>(Sounds.class);
	private static final LimiteDeVozes limite = new LimiteDeVozes();

	/** Lê a duração do WAV pelo cabeçalho. Na dúvida, meio segundo. */
	private static long duracaoEmMs(String caminho) {
		try {
			java.net.URL url = Sound.class.getResource(caminho);
			if (url != null) {
				try (javax.sound.sampled.AudioInputStream in =
						javax.sound.sampled.AudioSystem.getAudioInputStream(url)) {
					float taxa = in.getFormat().getFrameRate();
					long quadros = in.getFrameLength();
					if (taxa > 0 && quadros > 0) {
						return (long) (quadros * 1000L / taxa);
					}
				}
			}
		} catch (Exception semCabecalho) {
			// cai no padrão abaixo
		}
		return 500;
	}

	/** O efeito pode tocar agora, ou já há vozes dele demais? Ver LimiteDeVozes. */
	private static boolean cabeVoz(Sounds sound) {
		return limite.tocar(sound, System.currentTimeMillis(), duracoes.getOrDefault(sound, 500L));
	}

	/**
	 * Músicas ficam em OGG e são lidas em streaming.
	 *
	 * São faixas de minutos: em WAV somavam 67 MB, carregados inteiros na
	 * memória a cada abertura do jogo. Em Vorbis são 6 MB, e o streaming evita
	 * manter a faixa decodificada em RAM.
	 *
	 * O TinySound não conhece OGG; quem decodifica é o Service Provider do
	 * javax.sound declarado no build. Por isso a única mudança aqui é a
	 * extensão do arquivo.
	 */
	/**
	 * Carrega as faixas, a da tela inicial e a ambiência primeiro: são as que
	 * tocam logo depois da abertura. Roda na thread de fundo (ver load), sem segurar
	 * o lock do Sound — tocar um efeito não espera uma faixa decodificar.
	 */
	private static void loadMusics() {
		List<Musics> ordem = new java.util.ArrayList<>(List.of(Musics.Ambiente, Musics.Menu));
		for (Musics m : Musics.values()) {
			if (!ordem.contains(m)) {
				ordem.add(m);
			}
		}
		for(Musics name : ordem) {
			// Ogg por padrão; um nome com extensão (a trilha gerada da escolha de
			// elemento é WAV) é usado como está.
			String arquivo = name.resource().contains(".") ? name.resource() : name.resource() + ".ogg";
			Music music = TinySound.loadMusic(Engine.resPath + "audio/" + arquivo, true);
			if (music != null) {
				musics.put(name.resource(), music);
			}
		}
		musicasProntas = true;
	}
	
	/**
	 * O ganho de um efeito: a curva do slider de efeitos, vezes a do master.
	 *
	 * OS DOIS SE MULTIPLICAM porque sao perguntas diferentes. O de efeitos diz
	 * quanto os efeitos valem DENTRO da mixagem; o master diz quanto a mixagem
	 * inteira vale na sala em que voce esta. Somar seria misturar as duas coisas;
	 * multiplicar deixa o master mover tudo sem desfazer o equilibrio ajustado.
	 */
	private static double ganhoDeEfeito() {
		return Ganho.de(Configs.Volum()) * Ganho.de(Configs.Master());
	}

	/** O mesmo para a musica, abafada enquanto um menu esta aberto sobre o jogo. */
	static double ganhoDeMusica() {
		return Ganho.de(Configs.Music()) * Ganho.de(Configs.Master()) * fatorDaPausa;
	}

	/**
	 * Quanto da musica sobra com um menu aberto por cima do jogo.
	 *
	 * ABAFA, NAO CORTA. Hades e Dead Cells mantem a trilha tocando na pausa, so mais
	 * baixa: cortar faz a volta ao jogo comecar no silencio, e o silencio diz "a
	 * partida acabou", que e o contrario da pausa. Baixar ao suficiente para o menu
	 * ser ouvido por cima e o meio-termo. Ajuste fino: 0 corta, 1 deixa como esta.
	 */
	public static final double ABAFADO_NA_PAUSA = 0.35;

	private static double fatorDaPausa = 1d;

	/** A fracao de cada faixa pedida por {@link #volume}, para reaplicar depois. */
	private static final Map<Musics, Double> fracoes = new java.util.EnumMap<>(Musics.class);

	/**
	 * Liga ou desliga o abafamento da pausa. Idempotente: pode ser chamado a cada
	 * tick, e so mexe no volume quando o estado muda.
	 */
	public static synchronized void abafarParaPausa(boolean abafar) {
		double alvo = abafar ? ABAFADO_NA_PAUSA : 1d;
		if (alvo == fatorDaPausa) {
			return;
		}
		fatorDaPausa = alvo;
		updateVolumes();
	}

	public static void play(Sounds sound) {
		if(!sounds.containsKey(sound.resource()))
			throw new RuntimeException("sound not exists");
		if (!cabeVoz(sound)) {
			return;
		}
		sounds.get(sound.resource()).play(ganhoDeEfeito());
	}

	public static void play(Sounds sound, double pan) {
		if(!sounds.containsKey(sound.resource()))
			throw new RuntimeException("sound not exists");
		if (!cabeVoz(sound)) {
			return;
		}
		sounds.get(sound.resource()).play(ganhoDeEfeito(), pan);
	}
	
	public static void play(Musics music, boolean loop) {
		fracoes.put(music, 1d);
		Music m = musics.get(music.resource());
		if (m == null) {
			pendente(music, loop);
			return;
		}
		m.play(loop, ganhoDeMusica());
	}

	/** A faixa ainda carrega: toca quando chegar (ver tick). */
	private static synchronized void pendente(Musics music, boolean loop) {
		pendentes.put(music, loop);
	}

	/**
	 * Toca a faixa so se ela ja nao estiver tocando.
	 *
	 * Existe porque as arenas agora se encadeiam: com um play() direto, cada
	 * arena vencida rebobinava a trilha de combate para o comeco, o que denuncia
	 * a troca de mapa em vez de esconder.
	 */
	public static void keepPlaying(Musics music, boolean loop) {
		Music m = musics.get(music.resource());
		if (m == null) {
			synchronized (Sound.class) {
				if (!pendentes.containsKey(music)) {
					fracoes.put(music, 1d);
					pendentes.put(music, loop);
				}
			}
			return;
		}
		if(!m.playing()) {
			fracoes.put(music, 1d);
			m.play(loop, ganhoDeMusica());
		}
	}

	/**
	 * Volume de UMA faixa, como fracao do volume que o jogador escolheu.
	 *
	 * Serve a passagem cruzada entre duas trilhas: enquanto uma desce, a outra
	 * sobe. Sem isso a unica troca possivel e o corte — para uma, comeca a outra —
	 * e corte anuncia "mudou de estado" em vez de deixar o estado novo chegar.
	 *
	 * A fracao e sobre a preferencia do jogador, e nao um valor absoluto: quem
	 * jogou com a musica em vinte por cento continua ouvindo vinte por cento.
	 */
	public static void volume(Musics music, double fracao) {
		// UM DESVANECIMENTO EM CURSO MANDA. Quem pede um volume no meio dele (a
		// trilha da arena, que se ajusta sozinha) muda o destino, e não o volume
		// de agora: sem isso a faixa pularia no meio da rampa.
		synchronized (Sound.class) {
			double[] d = desvanecendo.get(music);
			if (d != null) {
				d[1] = Math.max(0d, Math.min(1d, fracao));
				return;
			}
		}
		aplicarVolume(music, fracao);
	}

	private static void aplicarVolume(Musics music, double fracao) {
		// A FRAÇÃO VALE MESMO PARA FAIXA QUE AINDA CARREGA: uma rampa de entrada
		// pedida antes de ela chegar faz a faixa começar no ponto certo da rampa.
		double f = Math.max(0d, Math.min(1d, fracao));
		fracoes.put(music, f);
		Music m = musics.get(music.resource());
		if(m != null) {
			m.setVolume(f * ganhoDeMusica());
		}
	}

	/** O volume atual da faixa, como fração da preferência do jogador. */
	public static synchronized double fracao(Musics music) {
		return fracoes.getOrDefault(music, 1d);
	}

	/**
	 * Desvanecimentos em curso, por faixa: {de, para, tick atual, ticks, parar no fim}.
	 *
	 * POR TICK DO ENGINE, E NÃO DE QUEM PEDIU. A troca de trilha da escolha de
	 * elemento começa numa tela que fecha antes de a música terminar de voltar; se
	 * a rampa morasse na tela, ela morreria com ela. Aqui ela anda até o fim,
	 * pausada ou não a partida (Engine.tick chama {@link #tick()}).
	 */
	private static final Map<Musics, double[]> desvanecendo = new java.util.EnumMap<>(Musics.class);

	/**
	 * Leva a faixa até {@code alvo} (fração da preferência do jogador) em
	 * {@code ticks}, numa rampa que soa linear (ver o meio do caminho em tick()).
	 * Com {@code parar}, a faixa para ao chegar — só faz sentido com alvo 0.
	 */
	public static synchronized void desvanecer(Musics music, double alvo, int ticks, boolean parar) {
		desvanecendo.put(music, new double[]{fracao(music), Math.max(0d, Math.min(1d, alvo)), 0,
				Math.max(1, ticks), parar ? 1 : 0});
	}

	/** Anda todos os desvanecimentos um tick. Chamado pelo Engine. */
	public static synchronized void tick() {
		if (!pendentes.isEmpty()) {
			java.util.Iterator<Map.Entry<Musics, Boolean>> p = pendentes.entrySet().iterator();
			while (p.hasNext()) {
				Map.Entry<Musics, Boolean> e = p.next();
				Music m = musics.get(e.getKey().resource());
				if (m != null) {
					m.play(e.getValue(), fracoes.getOrDefault(e.getKey(), 1d) * ganhoDeMusica());
					p.remove();
				}
			}
		}
		if (desvanecendo.isEmpty()) {
			return;
		}
		java.util.Iterator<Map.Entry<Musics, double[]>> it = desvanecendo.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Musics, double[]> e = it.next();
			double[] d = e.getValue();
			d[2]++;
			double t = Math.min(1, d[2] / d[3]);
			// Seno no tempo: a rampa sai e chega devagar, sem o "degrau" que o
			// ouvido nota no começo e no fim de uma reta.
			double suave = 0.5 - 0.5 * Math.cos(Math.PI * t);
			aplicarVolume(e.getKey(), d[0] + (d[1] - d[0]) * suave);
			if (t >= 1) {
				if (d[4] == 1) {
					stop(e.getKey());
				}
				it.remove();
			}
		}
	}

	public static void play(Musics music, boolean loop, double pan) {
		Music m = musics.get(music.resource());
		if (m == null) {
			pendente(music, loop);      // ainda carrega: chega no centro, sem o pan
			return;
		}
		m.play(loop, ganhoDeMusica(), pan);
	}

	public static void stop(Sounds sound) {
		if(!sounds.containsKey(sound.resource()))
			throw new RuntimeException("sound not exists");
		sounds.get(sound.resource()).stop();
	}

	/** Esta faixa esta tocando agora? */
	public static boolean playing(Musics music) {
		Music m = musics == null ? null : musics.get(music.resource());
		return m != null && m.playing();
	}

	/**
	 * Guarda o que estava tocando para a audicao de musica nas opcoes.
	 *
	 * Sem isto, experimentar as trilhas no menu ABERTO DENTRO DO JOGO deixava a
	 * partida muda ao voltar: a audicao parava a musica da arena e ninguem
	 * lembrava de religar. PAUSA, e nao para, entao a arena volta de onde estava
	 * em vez de rebobinar. Usa uma lista propria: silenciar()/retomar(), do foco
	 * da janela, pode acontecer no meio de uma audicao.
	 */
	public static synchronized void pausarParaAudicao() {
		if(musics == null) {
			return;
		}
		emAudicao.clear();
		for(Musics name : Musics.values()) {
			Music music = musics.get(name.resource());
			if(music != null && music.playing()) {
				music.pause();
				emAudicao.add(name);
			}
		}
	}

	/**
	 * Toca a faixa, RETOMANDO se ela so estava pausada.
	 *
	 * A diferenca importa: {@code stop()} da TinySound rebobina, e {@code play()}
	 * numa faixa parada comeca do zero. Retomar primeiro e o que permite escolher
	 * no menu a mesma musica que ja estava tocando sem ela voltar ao inicio.
	 */
	public static synchronized void tocarOuRetomar(Musics music) {
		Music m = musics == null ? null : musics.get(music.resource());
		if(m == null) {
			return;
		}
		m.resume();
		if(!m.playing()) {
			m.play(true, ganhoDeMusica());
		}
	}

	/**
	 * Encerra a audicao de musica das opcoes.
	 *
	 * POR QUE ISTO E DELICADO. A TinySound nao expoe a posicao da faixa: da para
	 * pausar e retomar, mas {@code stop()} REBOBINA e nao ha como voltar ao ponto.
	 * Entao a unica forma de a musica ficar continua e nunca parar o que se quer
	 * de volta — e era exatamente esse o defeito: a audicao pausava a trilha da
	 * arena e, logo em seguida, o "parar tudo de combate" dava stop nela. Voltava
	 * do comeco porque tinha sido rebobinada no meio do caminho.
	 *
	 * @param previa     a faixa que estava sendo ouvida, ou null
	 * @param previaFica quando true a previa CONTINUA tocando e vira a musica da
	 *                   partida. E o caso de estar dentro do jogo: assim nao ha
	 *                   corte nenhum entre ouvir no menu e voltar a jogar — a
	 *                   faixa simplesmente segue. As trilhas de briga antigas sao
	 *                   paradas, porque o jogador acabou de trocar de musica.
	 *                   Quando false — no menu principal — a previa e cortada e o
	 *                   que estava tocando antes volta de onde parou.
	 */
	public static synchronized void encerrarAudicao(Musics previa, boolean previaFica) {
		if(musics == null) {
			emAudicao.clear();
			return;
		}
		for(Musics nome : emAudicao) {
			Music m = musics.get(nome.resource());
			if(m == null || nome == previa) {
				continue;                      // a previa fica como esta
			}
			if(previaFica && Musics.deCombate(nome)) {
				m.stop();
				continue;
			}
			m.resume();
			if(!m.playing()) {
				m.play(true, ganhoDeMusica());
			}
		}
		emAudicao.clear();
		if(!previaFica && previa != null) {
			stop(previa);
		}
	}

	public static void stop(Musics music) {
		synchronized (Sound.class) {
			pendentes.remove(music);
		}
		Music m = musics.get(music.resource());
		if (m != null) {
			m.stop();
		}
	}

	public static void stopAllSounds() {
		if (sounds != null) {
			for (kuusisto.tinysound.Sound sound : sounds.values()) {
				sound.stop();
			}
		}
	}

	public static void stopAllMusics() {
		synchronized (Sound.class) {
			pendentes.clear();
		}
		for(Music music : musics.values()){
			music.stop();
		}
	}

	/** Faixas que estavam tocando quando a janela perdeu o foco. */
	private static final java.util.List<Musics> pausadas = new java.util.ArrayList<>();

	/**
	 * Cala o jogo enquanto a janela nao esta em foco.
	 *
	 * A musica seguia tocando com o jogo minimizado, ou mesmo antes de alguem ter
	 * clicado nele uma unica vez: o processo sobe, a trilha comeca, e quem esta
	 * em outra janela nao tem como saber de onde vem o som.
	 *
	 * As musicas sao PAUSADAS, e nao paradas, para voltarem de onde estavam. O
	 * volume global vai a zero junto porque efeitos disparados nesse meio tempo
	 * nao passam por essa lista.
	 */
	public static synchronized void silenciar() {
		if(musics == null) {
			return;
		}
		pausadas.clear();
		for(Musics name : Musics.values()) {
			Music music = musics.get(name.resource());
			if(music != null && music.playing()) {
				music.pause();
				pausadas.add(name);
			}
		}
		TinySound.setGlobalVolume(0d);
	}

	/**
	 * O som esta calado por falta de foco?
	 *
	 * Existe porque uma faixa PAUSADA responde "nao estou tocando", e quem decide
	 * a trilha pergunta exatamente isso a cada tick. Com a janela sem foco — o que
	 * acontece por um instante a cada F11, porque a janela e recriada — a arena
	 * concluia que a musica tinha acabado e a subia DE NOVO, do comeco. Era o
	 * reinicio que se ouvia a cada troca de tela cheia.
	 */
	public static synchronized boolean silenciado() {
		return !pausadas.isEmpty();
	}

	/** Devolve o som quando a janela volta ao foco. */
	public static synchronized void retomar() {
		TinySound.setGlobalVolume(1d);
		if(musics == null) {
			return;
		}
		for(Musics name : pausadas) {
			Music music = musics.get(name.resource());
			if(music != null) {
				music.resume();
			}
		}
		pausadas.clear();
	}

	public static void stopAll() {
		stopAllSounds();
		stopAllMusics();
		synchronized (Sound.class) {
			desvanecendo.clear();   // rampa de uma cena que acabou não sobe a próxima
		}
		// Quem para tudo esta trocando de cena (menu, fim de corrida): a proxima
		// musica nasce sem o abafamento de uma pausa que ja nao existe.
		fatorDaPausa = 1d;
	}

	/**
	 * Reaplica o volume de todas as faixas, mantendo a fracao de cada uma.
	 *
	 * A FRACAO FICA. Antes cada faixa voltava ao volume cheio, o que desfazia no
	 * meio um cruzamento da trilha da arena (uma subindo, a outra descendo) toda
	 * vez que alguem mexia no slider ou abria a pausa.
	 */
	public static void updateVolumes() {
		if (musics != null) {
			for (Musics nome : Musics.values()) {
				Music music = musics.get(nome.resource());
				if (music != null) {
					music.setVolume(fracoes.getOrDefault(nome, 1d) * ganhoDeMusica());
				}
			}
		}
	}



	public static void dispose() {
		TinySound.shutdown();
	}

}
