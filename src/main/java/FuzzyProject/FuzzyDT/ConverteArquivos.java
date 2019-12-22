package main.java.FuzzyProject.FuzzyDT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

public class ConverteArquivos {
    static int numExemplos;
    static String[] exemplos;
    static String[][] atribs;
    static int numAtribs;
    private static Object path;

    public ConverteArquivos() {
    }

    public void main(String arquivo) throws FileNotFoundException, IOException {
        String current = (new File(".")).getCanonicalPath();
        String arquivoOriginal = arquivo + ".dat";
        String arq = current + "/" + arquivo + "/" + arquivoOriginal;
        new File(current + "/" + arquivo);
        (new File(current + "/" + arquivo)).mkdir();
        File source = new File(arquivoOriginal);
        File dest = new File(arq);

        try {
            Path var8 = Files.copy(source.toPath(), dest.toPath());
        } catch (IOException var23) {
            System.err.println(var23.getMessage());
        }

        BufferedReader inReader = null;
        LineNumberReader lnr = new LineNumberReader(new FileReader(new File(arq)));
        long skip = lnr.skip(9223372036854775807L);
        int linhas = lnr.getLineNumber();

        try {
            inReader = new BufferedReader(new FileReader(arq));
        } catch (FileNotFoundException var22) {
            System.err.println("Unable to open file: " + arq);
            System.exit(1);
        }

        StringTokenizer str;
        int i;
        String temp;
        try {
            String line = inReader.readLine();
            str = new StringTokenizer(line);
            String nomeBaseDeDados = str.nextToken();
            nomeBaseDeDados = str.nextToken();
            atribs = new String[200][2];
            i = 0;
            line = inReader.readLine();
            str = new StringTokenizer(line);
            ++numAtribs;
            str.nextToken();

            int m;
            do {
                atribs[i][0] = str.nextToken();
                temp = str.nextToken();
                if (temp.contains("real")) {
                    atribs[i][1] = "real";
                } else if (temp.contains("integer")) {
                    atribs[i][1] = "integer";
                } else {
                    int y;
                    String valoresAtrib;
                    if (temp.contains("{")) {
                        y = str.countTokens();

                        for(m = 0; m < y; ++m) {
                            temp = temp + str.nextToken() + " ";
                        }

                        valoresAtrib = temp.replaceAll("\\{", "");
                        temp = valoresAtrib.replaceAll(",", " ");
                        valoresAtrib = temp.replaceAll("\\}", "");
                        atribs[i][1] = valoresAtrib;
                    } else {
                        y = str.countTokens();
                        valoresAtrib = temp + " ";

                        for(int h = 0; h < y; ++h) {
                            valoresAtrib = valoresAtrib + str.nextToken() + " ";
                        }

                        String temp2 = valoresAtrib.replace(",", " ");
                        atribs[i][1] = temp2;
                    }
                }

                ++i;
                line = inReader.readLine();
                str = new StringTokenizer(line);
                ++numAtribs;
            } while("@attribute".equals(str.nextToken()));

            line = inReader.readLine();
            new StringTokenizer(line);
            line = inReader.readLine();
            new StringTokenizer(line);
            line = inReader.readLine();
            str = new StringTokenizer(line);
            numExemplos = linhas - numAtribs - 3;
            exemplos = new String[numExemplos];
            temp = "";

            do {
                temp = temp + str.nextToken() + " ";
            } while(str.hasMoreTokens());

            String lixo = temp.replaceAll(",", "\t");
            exemplos[0] = lixo;
            temp = "";

            for(m = 1; m < numExemplos; ++m) {
                line = inReader.readLine();
                str = new StringTokenizer(line);

                do {
                    temp = temp + str.nextToken() + " ";
                } while(str.hasMoreTokens());

                lixo = temp.replaceAll(",", "\t");
                exemplos[m] = lixo;
                temp = "";
            }

            System.out.println("Database: " + nomeBaseDeDados);
            inReader.close();
        } catch (IOException var26) {
            System.err.println(var26.getMessage());
        }

        str = null;

        FileWriter writer;
        BufferedWriter buf_writer;
        try {
            writer = new FileWriter(current + "/" + arquivo + "/" + arquivo + ".txt");
            buf_writer = new BufferedWriter(writer);

            for(i = 0; i < numExemplos; ++i) {
                buf_writer.write(exemplos[i]);
                buf_writer.newLine();
            }

            buf_writer.close();
        } catch (IOException var25) {
            System.err.println(var25);
            System.exit(1);
        }

        str = null;

        try {
            writer = new FileWriter(current + "/" + arquivo + "/" + arquivo + ".names");
            buf_writer = new BufferedWriter(writer);

            for(i = 0; i < numAtribs - 1; ++i) {
                temp = atribs[i][0] + " " + atribs[i][1];
                buf_writer.write(temp);
                buf_writer.newLine();
            }

            buf_writer.close();
        } catch (IOException var24) {
            System.err.println(var24);
            System.exit(1);
        }

    }
}

