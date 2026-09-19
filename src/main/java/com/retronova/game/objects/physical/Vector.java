package com.retronova.game.objects.physical;

public class Vector {

    private final String id;
    private double vecX;
    private double vecY;
    private double angle;
    private double force;

    /**
     * O quanto ESTA forca se apaga por quadro, alem do atrito do chao.
     *
     * POR QUE A FORCA PRECISA DE UM FREIO SO DELA. Antes, quem quisesse um empurrao
     * que sumisse depressa nao tinha para onde ir a nao ser o {@code drag} do
     * Physical — e o drag descreve o CHAO, nao a forca: e ele que faz areia, lava e
     * atordoamento segurarem quem passa por cima. Usar o drag para apagar um
     * impulso e dizer ao jogo que o chao ficou pesado, e o jogo acredita: tudo o
     * que a entidade tentar fazer em seguida sai enfraquecido.
     *
     * Com o freio no proprio vetor, "este empurrao acaba rapido" deixa de ser
     * "o chao deste lugar e ruim".
     */
    private double freio;

    Vector(String id, double force, double angle) {
        this.id = id;
        this.vecX = Math.cos(angle);
        this.vecY = Math.sin(angle);
        this.angle = angle;
        this.force = force;
    }

    double getAngle() {
        return this.angle;
    }

    double getVecX() {
        return this.vecX * force;
    }

    void setAngle(double angle) {
        this.angle = angle;
        this.vecX = Math.cos(angle);
        this.vecY = Math.sin(angle);
    }

    double getVecY() {
        return this.vecY * force;
    }

    double getForce() {
        return this.force;
    }

    double getFreio() {
        return this.freio;
    }

    void setFreio(double freio) {
        this.freio = Math.max(0, Math.min(1, freio));
    }

    void setForce(double force) {
        this.force = force;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Vector vec) {
            return this.id.equals(vec.id);
        }
        return false;
    }

}
