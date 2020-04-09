package FuzzyProject.FuzzyDT.Models;

import FuzzyProject.FuzzyDT.Utils.ConverteArquivos;
import FuzzyProject.FuzzyDT.Utils.ManipulaArquivos;
import FuzzyProject.FuzzyND.Models.Exemplo;

import java.io.IOException;
import java.util.*;

public class ComiteArvores {

    public int tamanhoMaximo;
    public int numAtributos;
    public String dataset;
    public String caminho;
    public String taxaPoda;
    public int numCjtos;
    public List<String> atributos = new ArrayList<>();
    public List<DecisionTree> modelos = new ArrayList<>();
    public List<String> rotulosConhecidos = new ArrayList<>();
    public List<Integer> numeroClassificadores = new ArrayList<>();
    public Map<String, Integer> hashmapRotulos = new HashMap<>(); //para converter String para Integer

    public FDT fdt = new FDT();
    public ConverteArquivos ca = new ConverteArquivos();
    public ManipulaArquivos ma = new ManipulaArquivos();

    public ComiteArvores(String dataset, String caminho, String taxaPoda, int numCjtos, int tamanhoMaximo) {
        this.dataset = dataset;
        this.caminho = caminho;
        this.taxaPoda = taxaPoda;
        this.numCjtos = numCjtos;
        this.tamanhoMaximo = tamanhoMaximo;
    }

    public void treinaComiteInicial(int tChunk, int K) throws Exception {
        int qtdClassificadores = ca.main(this.dataset, this, tChunk);
        for(int i=0; i<qtdClassificadores; i++) {
            DecisionTree dt = new DecisionTree(this.caminho, this.dataset, i, this.taxaPoda);
            dt.numObjetos = ma.getNumExemplos(this.caminho+this.dataset + i + ".txt");
            dt.numAtributos = this.numAtributos;
            dt.atributos = this.atributos;
            fdt.geraFuzzyDT(this.dataset + i, this.taxaPoda, this.numCjtos, this.caminho, dt);
            fdt.criaGruposEmNosFolhas(this.dataset+i, this.caminho, dt, tChunk, K);
            ma.apagaArqsTemporarios(dataset + i, caminho);
            this.modelos.add(dt);
        }
    }

    public String classificaExemploVotoMajoritario(double[] exemplo) {
        Map<String, Integer> numeroVotos = new HashMap<>();
        for(int i=0; i<rotulosConhecidos.size(); i++) {
            numeroVotos.put(rotulosConhecidos.get(i), 0);
        }
        numeroVotos.put("desconhecido", 0);

        Vector v = new Vector<>();
        for(int i=0; i<exemplo.length; i++) {
            v.add(exemplo[i]);
        }

        for(int i=0; i<modelos.size(); i++) {
            String rotuloVotado = fdt.classificaExemplo(modelos.get(0), v);
            numeroVotos.replace(rotuloVotado, numeroVotos.get(rotuloVotado) + 1);
        }

        if(numeroVotos.get("desconhecido") == this.modelos.size()) {
            return "desconhecido";
        } else {
            numeroVotos.remove("desconhecido");
            int valorMaior = -1;
            String indiceMaior = null;

            for(int i=0; i<numeroVotos.size(); i++) {
                String rotulo = rotulosConhecidos.get(i);
                if(valorMaior < numeroVotos.get(rotulo)) {
                    valorMaior = numeroVotos.get(rotulo);
                    indiceMaior = rotulo;
                }
            }
            return indiceMaior;
        }
    }

    public void removeClassificadorComMenorDesempenho(List<Exemplo> exemplosRotulados) {
        double[] pontuacaoArvores = new double[this.modelos.size()];
        for(int i=0; i<this.modelos.size(); i++) {
            pontuacaoArvores[i] = 0;
        }

        for(int i=0; i<exemplosRotulados.size(); i++) {
            Vector v = new Vector<>();
            double[] exemplo = exemplosRotulados.get(i).getPoint();
            for(int j=0; j<exemplo.length; j++) {
                v.add(exemplo[j]);
            }
            for(int k=0; k<this.modelos.size(); k++) {
                String rotuloClassificado = this.fdt.classificaExemplo(this.modelos.get(k), v);
                String rotuloVerdadeiro = exemplosRotulados.get(i).getRotuloVerdadeiro();

                if(rotuloClassificado.equals(rotuloVerdadeiro)) {
                    pontuacaoArvores[k]++;
                }
            }
        }

        List<Double> acuraciaArvores = new ArrayList<>();
        for(int i=0; i<this.modelos.size(); i++) {
            acuraciaArvores.add(((pontuacaoArvores[i]/exemplosRotulados.size())*100));
        }

        System.out.println("TESTE DE NOVO");

    }

    private void atualizaRotulosConhecidos() {
        this.rotulosConhecidos.clear();
        for(int i=0; i<this.modelos.size(); i++) {
            for(int j=0; j<this.modelos.get(i).rotulos.size(); j++) {
                if(!this.rotulosConhecidos.contains(this.modelos.get(i).rotulos.get(j))) {
                    this.rotulosConhecidos.add(this.modelos.get(i).rotulos.get(j));
                }
            }
        }
    }

    public void treinaNovaArvore(List<Exemplo> exemplosRotulados, int tChunk, int K) throws Exception {
        int nClassificador = ca.mainParaExemplosRotulados(this.dataset, exemplosRotulados, this, tChunk);
        DecisionTree dt = new DecisionTree(this.caminho, this.dataset, nClassificador, this.taxaPoda);
        dt.numObjetos = ma.getNumExemplos(this.caminho+this.dataset + nClassificador + ".txt");
        dt.numAtributos = this.numAtributos;
        dt.atributos = this.atributos;
        fdt.geraFuzzyDT(this.dataset + nClassificador, this.taxaPoda, this.numCjtos, this.caminho, dt);
        fdt.criaGruposEmNosFolhas(this.dataset+nClassificador, this.caminho, dt, tChunk, K);
        ma.apagaArqsTemporarios(dataset + nClassificador, caminho);
        this.modelos.add(dt);
    }
}
