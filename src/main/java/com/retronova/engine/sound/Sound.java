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
