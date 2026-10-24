package com.retronova.game.save;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Um arquivo de save que não se perde: escrita atômica, backup e recuperação.
 *
 * GRAVAR:
 *  1. escreve tudo num ".tmp" ao lado do arquivo (mesma pasta, mesmo disco);
 *  2. o arquivo atual, se lê, vira o ".bak" — o último save que funcionou;
 *  3. o ".tmp" substitui o arquivo num movimento atômico (ATOMIC_MOVE), e onde o
 *     sistema não oferece isso, num REPLACE_EXISTING comum.
 * O temporário é forçado ao dispositivo antes da troca. A troca é atômica
 * quando suportada; o fallback depende do sistema de arquivos e do backup.
 * Isto não promete proteção contra qualquer falha de hardware.
 *
 * LER: tenta o arquivo; se ele não abre (ou o conteúdo não passa na leitura de
 * quem chamou), tenta o ".bak"; se nenhum serve, começa do zero. O arquivo que
 * não abriu NÃO é sobrescrito: vira ".corrompido", para dar para investigar e,
 * se for o caso, recuperar à mão. Começar do zero em silêncio por cima do
 * arquivo ruim é o erro que estas regras existem para evitar.
 */
public final class ArquivoSeguro {

    private ArquivoSeguro() {
    }

    /** Como a leitura terminou, para quem quiser avisar o jogador. */
    public enum Origem {
        /** O arquivo principal, normal. */
        PRINCIPAL,
        /** O principal não abriu; veio do backup. */
        BACKUP,
        /** Não havia arquivo: começo do zero, sem problema nenhum. */
        NOVO,
        /** Havia arquivo e nenhum abriu: começo do zero, com o ruim guardado ao lado. */
        PERDIDO
    }

    /** Converte o texto lido; lança exceção se o conteúdo não presta. */
    @FunctionalInterface
    public interface Leitor<T> {
        T ler(String texto) throws Exception;
    }

    public record Lido<T>(T valor, Origem origem) {
    }

    /** Não tratar um save de uma versão futura como corrupção. */
    public static final class VersaoIncompativel extends RuntimeException {
        public VersaoIncompativel(long versao) {
            super("Save de versao nao suportada: " + versao);
        }
    }

    public static Path backup(Path arquivo) {
        return arquivo.resolveSibling(arquivo.getFileName() + ".bak");
    }

    static Path temporario(Path arquivo) {
        return arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
    }

    /** Grava o texto com as três etapas acima. */
    public static void gravar(Path arquivo, String texto, Leitor<?> validar) throws IOException {
        try {
            validar.ler(texto);
        } catch (Exception invalido) {
            throw new IOException("Conteudo de save invalido", invalido);
        }
        Files.createDirectories(arquivo.toAbsolutePath().getParent());
        Path tmp = temporario(arquivo);
        try (FileChannel canal = FileChannel.open(tmp, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = StandardCharsets.UTF_8.encode(texto);
            while (bytes.hasRemaining()) {
                canal.write(bytes);
            }
            canal.force(true);
        }
        // Só o que lê vira backup: copiar um arquivo já estragado por cima do
        // .bak bom trocaria o último save válido por lixo.
        if (Files.exists(arquivo) && lePor(arquivo, validar)) {
            Path bak = backup(arquivo);
            Path bakTmp = temporario(bak);
            Files.copy(arquivo, bakTmp, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel canal = FileChannel.open(bakTmp, StandardOpenOption.WRITE)) {
                canal.force(true);
            }
            mover(bakTmp, bak);
        }
        mover(tmp, arquivo);
    }

    private static void mover(Path tmp, Path arquivo) throws IOException {
        try {
            Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException semAtomico) {
            Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Lê o arquivo, ou o backup, ou nada; ver o cabeçalho. */
    public static <T> Lido<T> ler(Path arquivo, Leitor<T> leitor) {
        boolean existia = Files.exists(arquivo);
        if (existia) {
            try {
                return new Lido<>(leitor.ler(texto(arquivo)), Origem.PRINCIPAL);
            } catch (VersaoIncompativel futura) {
                throw futura;
            } catch (IOException inacessivel) {
                throw new UncheckedIOException("Nao foi possivel ler " + arquivo, inacessivel);
            } catch (Exception ruim) {
                System.err.println("[save] " + arquivo.getFileName() + " ilegivel: " + ruim.getMessage());
                guardarCorrompido(arquivo);
            }
        }
        Path bak = backup(arquivo);
        if (Files.exists(bak)) {
            try {
                T valor = leitor.ler(texto(bak));
                System.err.println("[save] " + arquivo.getFileName() + " recuperado do backup.");
                return new Lido<>(valor, Origem.BACKUP);
            } catch (VersaoIncompativel futura) {
                throw futura;
            } catch (IOException inacessivel) {
                throw new UncheckedIOException("Nao foi possivel ler " + bak, inacessivel);
            } catch (Exception tambemRuim) {
                System.err.println("[save] backup de " + arquivo.getFileName() + " tambem ilegivel.");
            }
        }
        return new Lido<>(null, existia ? Origem.PERDIDO : Origem.NOVO);
    }

    private static boolean lePor(Path arquivo, Leitor<?> validar) {
        try {
            validar.ler(texto(arquivo));
            return true;
        } catch (Exception nao) {
            return false;
        }
    }

    private static String texto(Path arquivo) throws IOException {
        String t = Files.readString(arquivo, StandardCharsets.UTF_8);
        return t.startsWith("﻿") ? t.substring(1) : t;   // BOM de editor de texto
    }

    private static void guardarCorrompido(Path arquivo) {
        try {
            Path destino = arquivo.resolveSibling(arquivo.getFileName() + ".corrompido");
            if (Files.exists(destino)) {
                destino = arquivo.resolveSibling(arquivo.getFileName() + ".corrompido-" + System.nanoTime());
            }
            Files.move(arquivo, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[save] nao consegui separar o arquivo ruim: " + e.getMessage());
        }
    }
}
