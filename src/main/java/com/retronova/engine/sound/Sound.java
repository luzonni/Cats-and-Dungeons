package com.retronova.engine.sound;

import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import kuusisto.tinysound.Music;
import kuusisto.tinysound.TinySound;

import java.util.HashMap;
import java.util.Map;

public class Sound {
	
	private static Map<String, kuusisto.tinysound.Sound> sounds;
	private static Map<String, Music> musics;
	/** O que a audicao das opcoes pausou. Ver pausarParaAudicao. */
	private static final java.util.List<Musics> emAudicao = new java.util.ArrayList<>();

	public static synchronized void load() {
		TinySound.init();
        if(!TinySound.isInitialized())
			throw new RuntimeException("Error on load sounds!");
		TinySound.setGlobalVolume(1f);
		if(sounds == null)
			loadSounds();
		if(musics == null)
			loadMusics();
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
			kuusisto.tinysound.Sound sound = TinySound.loadSound(Engine.resPath + "audio/" + name.resource() + ".wav");
			sounds.put(name.resource(), sound);
		}
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
	private static synchronized void loadMusics() {
		musics = new HashMap<>();
		Musics[] names = Musics.values();
		for(Musics name : names) {
			Music music = TinySound.loadMusic(Engine.resPath + "audio/" + name.resource() + ".ogg", true);
			musics.put(name.resource(), music);
		}
	}
	
	public static void play(Sounds sound) {
		if(!sounds.containsKey(sound.resource()))
			throw new RuntimeException("sound not exists");
		sounds.get(sound.resource()).play((double) Configs.Volum() / 100d);
	}

	public static void play(Sounds sound, double pan) {
		if(!sounds.containsKey(sound.resource()))
			throw new RuntimeException("sound not exists");
		sounds.get(sound.resource()).play((double) Configs.Volum() / 100d, pan);
	}
	
	public static void play(Musics music, boolean loop) {
		if(!musics.containsKey(music.resource()))
			throw new RuntimeException("sound not exists");
		musics.get(music.resource()).play(loop, (double) Configs.Music() / 100d);
	}

	/**
	 * Toca a faixa so se ela ja nao estiver tocando.
	 *
	 * Existe porque as arenas agora se encadeiam: com um play() direto, cada
	 * arena vencida rebobinava a trilha de combate para o comeco, o que denuncia
	 * a troca de mapa em vez de esconder.
	 */
	public static void keepPlaying(Musics music, boolean loop) {
		if(!musics.containsKey(music.resource()))
			throw new RuntimeException("sound not exists");
		Music m = musics.get(music.resource());
		if(!m.playing()) {
			m.play(loop, (double) Configs.Music() / 100d);
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
		if(musics == null) {
			return;
		}
		Music m = musics.get(music.resource());
		if(m != null) {
			m.setVolume(Math.max(0d, Math.min(1d, fracao)) * Configs.Music() / 100d);
		}
	}

	public static void play(Musics music, boolean loop, double pan) {
		if(!musics.containsKey(music.resource()))
			throw new RuntimeException("sound not exists");
		musics.get(music.resource()).play(loop, (double) Configs.Music() / 100d, pan);
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
			m.play(true, (double) Configs.Music() / 100d);
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
				m.play(true, (double) Configs.Music() / 100d);
			}
		}
		emAudicao.clear();
		if(!previaFica && previa != null) {
			stop(previa);
		}
	}

	public static void stop(Musics music) {
		if(!musics.containsKey(music.resource()))
			throw new RuntimeException("sound not exists");
		musics.get(music.resource()).stop();
	}

	public static void stopAllSounds() {
		if (sounds != null) {
			for (kuusisto.tinysound.Sound sound : sounds.values()) {
				sound.stop();
			}
		}
	}

	public static void stopAllMusics() {
		if (musics != null) {
			for(Music music : musics.values()){
				music.stop();
			}
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
	}

	public static void updateVolumes() {
		if (musics != null) {
			for (Music music : musics.values()) {
				double volume = (double) Configs.Music() / 100d;
				music.setVolume(volume);
			}
		}
	}



	public static void dispose() {
		TinySound.shutdown();
	}

}
