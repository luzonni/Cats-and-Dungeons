import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Roda a CI inteira aqui, antes do push.
 *
 *     java tools/CiLocal.java
 *     java tools/CiLocal.java --rapido    # pula o fat-jar
 *
 * Descobrir no GitHub custa caro: o push falha, chega e-mail, e o conserto vira
 * mais um commit de "fix ci". Aqui o erro aparece antes de sair da maquina. O
 * hook em .githooks/pre-push chama este programa; para pular num caso pontual,
 * git push --no-verify.
 *
 * O VALOR NAO E SO REPETIR OS COMANDOS DO ci.yml — e reproduzir o que ele
 * enxerga. Dois passos aqui nao existem la, e sao justamente os que pegam o que
 * rodar os mesmos comandos na arvore de trabalho nao pega:
 *
 *  - NENHUM FONTE IGNORADO. A CI roda num CLONE. Um arquivo que existe so na
 *    sua maquina porque o .gitignore o esconde compila aqui e nao existe la, e
 *    nem o git status denuncia — ele omite ignorados.
 *
 *  - ESPELHO DO ci.yml. Guarda contra o defeito obvio deste arquivo: alguem
 *    acrescenta um passo no workflow, esquece daqui, e este programa volta a
 *    mentir que esta tudo verde.
 *
 * E o passo dos assets NAO copia o comando da CI. La o diff contra o HEAD serve
 * porque a arvore acabou de ser clonada e esta limpa; aqui ela quase nunca esta,
 * e um diff acusaria qualquer trabalho em andamento. O que se mede e se REGERAR
 * MUDA OS ARQUIVOS — que e a pergunta que a CI de fato faz.
 */
public class CiLocal {

    private static final String VERDE = "[32m";
    private static final String VERMELHO = "[31m";
    private static final String CINZA = "[90m";
    private static final String FIM = "[0m";

    /** Pastas cujo conteudo o build consome. */
    private static final String[] PASTAS_DE_CODIGO = {"src", "tools", "gradle"};

    private static final String UI =
            "src/main/resources/com/retronova/resources/ui";

    /**
     * Passos do ci.yml que nao tem como rodar aqui, e nem faria sentido.
     *
     * Sao os de infraestrutura do runner — preparar a maquina e publicar
     * artefato. O espelho ignora estes e cobra todos os outros.
     */
    private static final Set<String> SO_NO_GITHUB = Set.of(
            "checkout", "configurar jdk 21", "configurar gradle",
            "publicar relatorio de testes", "publicar artefato");

    private record Resultado(boolean ok, String saida) {
        static Resultado bom() {
            return new Resultado(true, "");
        }
        static Resultado ruim(String porque) {
            return new Resultado(false, porque);
        }
    }

    private record Passo(String nome, boolean lento, Supplier<Resultado> executar) { }

    private static Path raiz;

    public static void main(String[] args) throws Exception {
        boolean rapido = List.of(args).contains("--rapido");
        raiz = Path.of("").toAbsolutePath();
        if (!Files.exists(raiz.resolve("settings.gradle.kts"))) {
            System.err.println("Rode a partir da raiz do repositorio.");
            System.exit(2);
        }

        List<Passo> passos = List.of(
                new Passo("Nenhum fonte ignorado pelo git", false, CiLocal::fontesIgnorados),
                new Passo("Build", true, () -> gradle("build", "--stacktrace")),
                // A CI roda os testes com AWT desligado porque o runner nao tem
                // display. Aqui tem — e um teste que so passa por causa disso
                // passaria localmente e quebraria la.
                new Passo("Testes", true, () -> gradleHeadless("test")),
                new Passo("Regerar e comparar", false, CiLocal::assetsEmDia),
                new Passo("Gerar jar executavel", true, () -> gradle("fatJar")),
                new Passo("Espelho do ci.yml", false, CiLocal::espelho));

        System.out.println();
        System.out.println("Rodando a CI local" + (rapido ? " (--rapido: sem o fat-jar)" : ""));
        System.out.println();

        boolean falhou = false;
        for (Passo p : passos) {
            if (rapido && p.lento() && p.nome().equals("Gerar jar executavel")) {
                System.out.println(CINZA + "  --  " + p.nome() + " (pulado)" + FIM);
                continue;
            }
            long inicio = System.nanoTime();
            Resultado r = p.executar().get();
            String tempo = String.format(Locale.ROOT, "%.1fs", (System.nanoTime() - inicio) / 1e9);

            if (r.ok()) {
                System.out.println("  " + VERDE + "ok" + FIM + "  " + p.nome()
                        + " " + CINZA + tempo + FIM);
            } else {
                falhou = true;
                System.out.println("  " + VERMELHO + "XX" + FIM + "  " + p.nome()
                        + " " + CINZA + tempo + FIM);
                ultimasLinhas(r.saida(), 25);
                // Falha rapido: seguir depois de um build quebrado so gasta tempo.
                break;
            }
        }

        System.out.println();
        if (falhou) {
            System.out.println(VERMELHO + "A CI falharia." + FIM + " Corrija antes de empurrar.");
            System.out.println();
            System.exit(1);
        }
        System.out.println(VERDE + "Tudo verde." + FIM + " A CI deve passar.");
        System.out.println();
    }

    // ------------------------------------------------------------------ passos

    /**
     * Nenhum arquivo de codigo escondido pelo .gitignore.
     *
     * O que o git ignora nao vai no clone, e e no clone que a CI roda. Um padrao
     * amplo demais — "logs" em vez de "/logs/", que casa em qualquer
     * profundidade — some com uma pasta inteira sem ninguem notar, porque aqui o
     * arquivo esta no disco e tudo compila.
     */
    private static Resultado fontesIgnorados() {
        List<String> cmd = new ArrayList<>(List.of("git", "ls-files", "--others", "--ignored",
                "--exclude-standard", "--directory", "--"));
        cmd.addAll(List.of(PASTAS_DE_CODIGO));
        Saida s = rodar(cmd, Map.of());
        if (s.codigo() != 0) {
            return Resultado.ruim("git falhou:\n" + s.texto());
        }
        List<String> escondidos = s.texto().lines().map(String::trim)
                .filter(l -> !l.isEmpty()).toList();
        if (escondidos.isEmpty()) {
            return Resultado.bom();
        }
        StringBuilder sb = new StringBuilder("Estes caminhos existem aqui e NAO existem num clone limpo:\n");
        escondidos.forEach(c -> sb.append("  ").append(c).append('\n'));
        sb.append("\nRode `git check-ignore -v <caminho>` para ver qual regra pegou,\n");
        sb.append("e ancore o padrao (`/build/` em vez de `build`).");
        return Resultado.ruim(sb.toString());
    }

    /**
     * Os PNG de UI batem com o gerador.
     *
     * Mede o que a CI de fato pergunta: REGERAR muda alguma coisa? La ela usa
     * `git diff` porque a arvore vem limpa do checkout; aqui a arvore quase nunca
     * esta limpa, e um diff acusaria trabalho em andamento que nao tem nada a ver
     * com o gerador. Comparar antes e depois vale nos dois casos.
     */
    private static Resultado assetsEmDia() {
        Map<String, String> antes = impressoes();
        Saida s = rodar(List.of(javaBin(), "tools/GenUiAssets.java"), Map.of());
        if (s.codigo() != 0) {
            return Resultado.ruim("o gerador falhou:\n" + s.texto());
        }
        Map<String, String> depois = impressoes();

        List<String> mudaram = new ArrayList<>();
        for (String nome : new LinkedHashSet<>(depois.keySet())) {
            if (!depois.get(nome).equals(antes.get(nome))) {
                mudaram.add(nome);
            }
        }
        if (mudaram.isEmpty()) {
            return Resultado.bom();
        }
        return Resultado.ruim("Regerar mudou estes PNG, entao o que esta versionado esta velho:\n  "
                + String.join("\n  ", mudaram)
                + "\n\nOs arquivos JA foram corrigidos agora. Basta fazer commit deles.");
    }

    /**
     * Todo passo do ci.yml existe aqui.
     *
     * Compara sem acento e sem caixa: o workflow e escrito em portugues com
     * acentuacao, e depender de o javac ler este arquivo na mesma codificacao do
     * YAML seria mais uma coisa para quebrar numa maquina diferente.
     */
    private static Resultado espelho() {
        Path yml = raiz.resolve(".github/workflows/ci.yml");
        String texto;
        try {
            texto = Files.readString(yml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Resultado.ruim("nao consegui ler " + yml + ": " + e.getMessage());
        }
        Set<String> aqui = new LinkedHashSet<>();
        aqui.add(normalizar("Nenhum fonte ignorado pelo git"));
        aqui.add(normalizar("Build"));
        aqui.add(normalizar("Testes"));
        aqui.add(normalizar("Regerar e comparar"));
        aqui.add(normalizar("Gerar jar executavel"));
        aqui.add(normalizar("Espelho do ci.yml"));

        List<String> faltando = new ArrayList<>();
        Matcher m = Pattern.compile("^\\s+- name: (.+)$", Pattern.MULTILINE).matcher(texto);
        while (m.find()) {
            String nome = m.group(1).trim();
            String chave = normalizar(nome);
            if (SO_NO_GITHUB.contains(chave) || aqui.contains(chave)) {
                continue;
            }
            faltando.add(nome);
        }
        if (faltando.isEmpty()) {
            return Resultado.bom();
        }
        return Resultado.ruim("passos do ci.yml que nao existem aqui: " + String.join(", ", faltando)
                + "\n\nAcrescente-os em tools/CiLocal.java, ou a SO_NO_GITHUB se forem"
                + "\ninfraestrutura do runner que nao faz sentido rodar na maquina.");
    }

    // ------------------------------------------------------------------ apoio

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /** MD5 de cada PNG da pasta de UI, por nome. */
    private static Map<String, String> impressoes() {
        Map<String, String> mapa = new HashMap<>();
        File[] arquivos = raiz.resolve(UI).toFile().listFiles((d, n) -> n.endsWith(".png"));
        if (arquivos == null) {
            return mapa;
        }
        for (File f : arquivos) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] h = md.digest(Files.readAllBytes(f.toPath()));
                StringBuilder sb = new StringBuilder();
                for (byte b : h) {
                    sb.append(String.format("%02x", b));
                }
                mapa.put(f.getName(), sb.toString());
            } catch (Exception e) {
                mapa.put(f.getName(), "erro:" + e.getMessage());
            }
        }
        return mapa;
    }

    private static Resultado gradle(String... args) {
        return doGradle(Map.of(), args);
    }

    /** Como na CI: sem display, para um teste nao passar aqui e quebrar la. */
    private static Resultado gradleHeadless(String... args) {
        return doGradle(Map.of("JAVA_TOOL_OPTIONS", "-Djava.awt.headless=true"), args);
    }

    private static Resultado doGradle(Map<String, String> ambiente, String... args) {
        List<String> cmd = new ArrayList<>();
        if (windows()) {
            // O wrapper do Windows e um .bat, e .bat so roda por dentro do cmd.
            // Caminho ABSOLUTO: o cmd nem sempre procura no diretorio corrente —
            // depende de NoDefaultCurrentDirectoryInExePath —, e o erro que
            // aparece quando nao procura e "nao e reconhecido como um comando",
            // que nao parece ter nada a ver com isso.
            cmd.addAll(List.of("cmd.exe", "/c",
                    raiz.resolve("gradlew.bat").toString()));
        } else {
            cmd.add(raiz.resolve("gradlew").toString());
        }
        cmd.addAll(List.of(args));
        Saida s = rodar(cmd, ambiente);
        return s.codigo() == 0 ? Resultado.bom() : Resultado.ruim(s.texto());
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** O mesmo java que esta rodando este programa. */
    private static String javaBin() {
        return Path.of(System.getProperty("java.home"), "bin",
                windows() ? "java.exe" : "java").toString();
    }

    private record Saida(int codigo, String texto) { }

    /** Codificacao do console do sistema, com UTF-8 de reserva. */
    private static java.nio.charset.Charset nativa() {
        try {
            return java.nio.charset.Charset.forName(
                    System.getProperty("native.encoding", "UTF-8"));
        } catch (Exception naoConhecida) {
            return StandardCharsets.UTF_8;
        }
    }

    private static Saida rodar(List<String> comando, Map<String, String> ambiente) {
        try {
            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.directory(raiz.toFile());
            pb.redirectErrorStream(true);
            pb.environment().putAll(ambiente);
            Process p = pb.start();
            // A saida vem na codificacao do SISTEMA, nao em UTF-8: no Windows o
            // cmd fala cp850, e lida como UTF-8 a mensagem de erro sai cheia de
            // interrogacao justo quando mais se precisa dela.
            String saida = new String(p.getInputStream().readAllBytes(), nativa());
            return new Saida(p.waitFor(), saida);
        } catch (Exception e) {
            return new Saida(1, "nao consegui executar " + String.join(" ", comando)
                    + ": " + e);
        }
    }

    /** So o fim da saida: e onde o erro de verdade aparece. */
    private static void ultimasLinhas(String texto, int quantas) {
        List<String> linhas = texto.lines().filter(l -> !l.isBlank()).toList();
        int de = Math.max(0, linhas.size() - quantas);
        for (String l : linhas.subList(de, linhas.size())) {
            System.out.println("      " + l);
        }
    }
}
