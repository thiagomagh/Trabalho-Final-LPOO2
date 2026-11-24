package sistemapizzaria.formas;

import sistemapizzaria.excecoes.*;

public class Circulo extends Forma {

    private double raio;
    // Estabelece o limite mínimo aceitável para o raio do círculo.
    public static final double MIN_RAIO = 7.0;
    // Estabelece o limite máximo aceitável para o raio do círculo.
    public static final double MAX_RAIO = 23.0;

    // Configura o valor do raio do círculo mediante validação prévia dos limites dimensionais.
    @Override
    public void definirPorDimensao(double dimensao) {
        // Verifica se a dimensão fornecida (inclusive valores não numéricos) ultrapassa os limites mínimo ou máximo estabelecidos.
        if (Double.isNaN(dimensao) || dimensao < MIN_RAIO || dimensao > MAX_RAIO) {
            // Lança uma exceção para interromper a operação e notificar a camada superior sobre a dimensão inválida.
            throw new DimensaoInvalidaException("O raio do círculo deve ter entre 7 e 23 cm.");
        }
        // Atribui o valor validado ao atributo interno da classe.
        this.raio = dimensao;
    }

    // Calcula o raio necessário para atingir a área informada e configura a dimensão se o resultado estiver dentro dos limites.
    @Override
    public void definirPorArea(double area) {
        // Verifica se a área solicitada está dentro da faixa que o círculo aceita (limites globais de área e limites do raio).
        // Como o raio mínimo é 7 cm, áreas abaixo de ~153,94 cm² não são possíveis para o círculo, mesmo sendo >= 100 cm².
        if (Double.isNaN(area) || area < getAreaMinima() || area > getAreaMaxima()) {
            // Lança uma exceção indicando que a área solicitada está fora do padrão permitido.
            throw new AreaInvalidaException("A área de uma pizza circular deve ter entre " + descreverFaixaDeArea() + ".");
        }

        // Dentro da faixa de área aceita o raio sempre respeita os limites; o ajuste abaixo apenas protege
        // contra imprecisão de ponto flutuante nas bordas.
        double raioCalculado = Math.sqrt(area / Math.PI);
        this.raio = Math.min(MAX_RAIO, Math.max(MIN_RAIO, raioCalculado));
    }

    // Processa e devolve o valor da área atual do círculo
    @Override
    public double calcularArea() {
        return Math.PI * Math.pow(this.raio, 2);
    }

    @Override
    public double getAreaMinima() {
        return Math.max(MIN_AREA, Math.PI * MIN_RAIO * MIN_RAIO);
    }

    @Override
    public double getAreaMaxima() {
        return Math.min(MAX_AREA, Math.PI * MAX_RAIO * MAX_RAIO);
    }

    // Retorna o valor atualizado do raio do círculo.
    public double getRaio() {
        return this.raio;
    }
}
