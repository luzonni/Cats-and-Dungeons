package com.retronova.game.save;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O save permanente: o que nunca pode se perder nem se duplicar.
 *
 * Cada teste usa uma pasta temporária (Progresso.usarPasta), e a bancada é
 * desligada durante o teste: com -Dcd.bench o Progresso não grava, e outros
 * testes da suíte ligam a bancada.
 */
class ProgressoTest {

    @TempDir
    Path pasta;

    private String bancada;

    @BeforeEach
    void preparar() {
        bancada = System.getProperty("cd.bench");
        System.clearProperty("cd.bench");
        Progresso.usarPasta(pasta);
    }

    @AfterEach
    void restaurar() {
        if (bancada != null) {
            System.setProperty("cd.bench", bancada);
        }
    }

    private void recarregar() {
        Progresso.usarPasta(pasta);
    }

    @Test
    @DisplayName("Gravar e carregar devolve as mesmas moedas e melhorias, por gato")
    void idaEVolta() {
        Progresso.creditar("corrida-1", "Muffin", 150);
        assertTrue(Progresso.comprar("Muffin", "vida", 40, 5));
        recarregar();
        assertEquals(110, Progresso.moedas("Muffin"));
        assertEquals(1, Progresso.nivel("Muffin", "vida"));
        assertEquals(0, Progresso.moedas("Finn"), "as moedas sao do gato que jogou");
        assertEquals(ArquivoSeguro.Origem.PRINCIPAL, Progresso.origem());
    }

    @Test
    @DisplayName("A mesma corrida é creditada uma vez só, mesmo depois de reabrir")
    void creditoUnico() {
        assertEquals(80, Progresso.creditar("abc", "Azrael", 80));
        assertEquals(0, Progresso.creditar("abc", "Azrael", 80));
        recarregar();
        assertEquals(0, Progresso.creditar("abc", "Azrael", 80));
        assertEquals(80, Progresso.moedas("Azrael"));
        assertTrue(Progresso.foiCreditada("abc"));
    }

    @Test
    @DisplayName("Corrida de zero moedas também fica marcada como creditada")
    void creditoZero() {
        assertEquals(0, Progresso.creditar("vazia", "Finn", 0));
        recarregar();
        assertTrue(Progresso.foiCreditada("vazia"));
    }

    @Test
    @DisplayName("O meta.json antigo migra: níveis para cada gato, ouro dividido, original preservado")
    void migracao() throws Exception {
        Files.writeString(pasta.resolve("meta.json"), "{\"ouro\":100,\"VIGOR\":2,\"IMA\":1,\"FORCA\":0}");
        recarregar();
        for (String gato : Progresso.GATOS) {
            assertEquals(34, Progresso.moedas(gato), gato);
            assertEquals(2, Progresso.nivel(gato, "vida"), gato);
            assertEquals(1, Progresso.nivel(gato, "coleta"), gato);
            assertEquals(0, Progresso.nivel(gato, "dano"), gato);
        }
        assertTrue(Files.exists(pasta.resolve("progresso.json")));
        assertTrue(Files.exists(pasta.resolve("meta.json.migrado")));
        assertFalse(Files.exists(pasta.resolve("meta.json")));
    }

    @Test
    @DisplayName("Arquivo corrompido: volta pelo backup e guarda o ruim ao lado")
    void corrompido() throws Exception {
        Progresso.creditar("um", "Finn", 50);
        Progresso.creditar("dois", "Finn", 30);   // a segunda gravacao cria o .bak com 50
        Files.writeString(pasta.resolve("progresso.json"), "{isto nao e json");
        recarregar();
        assertEquals(ArquivoSeguro.Origem.BACKUP, Progresso.origem());
        assertEquals(50, Progresso.moedas("Finn"));
        assertTrue(Files.exists(pasta.resolve("progresso.json.corrompido")));
    }

    @Test
    @DisplayName("Principal e backup estragados: começa do zero sem apagar a prova")
    void tudoPerdido() throws Exception {
        Files.writeString(pasta.resolve("progresso.json"), "lixo");
        Files.writeString(pasta.resolve("progresso.json.bak"), "lixo tambem");
        recarregar();
        assertEquals(ArquivoSeguro.Origem.PERDIDO, Progresso.origem());
        assertEquals(0, Progresso.moedas("Muffin"));
        assertTrue(Files.exists(pasta.resolve("progresso.json.corrompido")));
    }

    @Test
    @DisplayName("Versão desconhecida não é lida como se fosse a atual")
    void versaoDesconhecida() throws Exception {
        Files.writeString(pasta.resolve("progresso.json"), "{\"versao\":99,\"gatos\":{}}");
        recarregar();
        assertEquals(ArquivoSeguro.Origem.PERDIDO, Progresso.origem());
    }

    @Test
    @DisplayName("Apagar a progressão zera tudo mas não deixa recreditar corrida velha")
    void apagar() {
        Progresso.creditar("velha", "Muffin", 90);
        Progresso.apagarTudo();
        assertEquals(0, Progresso.moedas("Muffin"));
        assertEquals(0, Progresso.creditar("velha", "Muffin", 90));
    }

    @Test
    @DisplayName("Comprar respeita saldo e nível máximo")
    void compra() {
        Progresso.creditar("x", "Azrael", 50);
        assertFalse(Progresso.comprar("Azrael", "dano", 60, 5), "sem saldo");
        assertTrue(Progresso.comprar("Azrael", "dano", 50, 1));
        assertFalse(Progresso.comprar("Azrael", "dano", 0, 1), "no maximo");
    }

    @Test
    @DisplayName("Moedas de uma corrida: tempo, abates, vitória e a fração do resultado")
    void regras() {
        RegrasDeMoedas r = RegrasDeMoedas.PADRAO;
        assertEquals(110, r.moedas(RegrasDeMoedas.Resultado.MORTE, 600, 80, 1));
        assertEquals(310, r.moedas(RegrasDeMoedas.Resultado.VITORIA, 600, 80, 1));
        assertEquals(55, r.moedas(RegrasDeMoedas.Resultado.ABANDONO, 600, 80, 1));
        assertEquals(165, r.moedas(RegrasDeMoedas.Resultado.MORTE, 600, 80, 1.5));
        assertEquals(RegrasDeMoedas.PADRAO, RegrasDeMoedas.atuais(), "o arquivo e os padroes concordam");
    }
}
