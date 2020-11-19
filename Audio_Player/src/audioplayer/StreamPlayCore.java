package audioplayer;

import network.Client;
import network.Downloader;
import network.Server;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class StreamPlayCore extends PlayCore {
    Client client;
    String music;
    int bufferSize;
    String[] givenHost;
    int[] givenPort;

    public StreamPlayCore(String music, int bufferSize, Client client,
                          String[] givenHost, int[] givenPort){
        super();
        this.client = client;
        this.music = music;
        this.bufferSize = bufferSize;
        this.givenHost = givenHost;
        this.givenPort = givenPort;
    }

    public void add(FileInputStream fis){
        this.input = new SequenceInputStream(this.input, fis);
    }

    @Override
    public void run() {
        try {
            String bufferPath = "database//buffer//";
            client.sendDownloadRequest(music, givenHost);
            Downloader d =  client.downloader.get(0);
            while(!d.getDownloadState(0)) {
                Thread.sleep(50);
            }
            //FileInputStream fis = new FileInputStream("database//buffer//" + music + "//1");
            //this.input = new SequenceInputStream(fis, null);
            this.player = new WavPlayer(bufferPath + music.split("\\.")[0] + "//1");
            this.input = this.player.head.getAudio();
            this.player.initialDataLine();

            byte[] head = new byte[this.player.head.headSize];
            FileInputStream fis = new FileInputStream(bufferPath + music.split("\\.")[0] + "//1");
            fis.read(head, 0, head.length);
            fis.close();
            File newSongFile = new File("database//" + music);
            FileOutputStream newSong = new FileOutputStream(newSongFile);
            newSong.write(head);
            int lineBufferSize = this.player.audioLine.getBufferSize();

            this.player.playLock = new Object();
            synchronized (this.player.playLock) {
                this.player.playState = true;
                this.player.audioLine.start();
                byte[] data = new byte[bufferSize];

                int nextBuffer = 1;
                int dataRead;
                while ((dataRead = input.read(data)) != -1
                        && this.flag) {

                    synchronized (this.player.restTime) {
                        if (this.player.head != null) this.player.restTime.time = this.player.audioLine.getMicrosecondPosition() / 1000000.0;
                    }
                    while (!this.player.playState) {
                        this.player.playLock.wait();
                    }

                    if(dataRead % bufferSize != 0){
                        byte[] offset = new byte[bufferSize - dataRead];
                        int concat = input.read(offset, 0, bufferSize - dataRead);
                        for(int m = dataRead; m < dataRead + concat; m++){
                            data[m] = offset[m - dataRead];
                        }
                        dataRead += concat;
                    }

                    this.player.audioLine.write(data, 0, dataRead);
                    newSong.write(data, 0, dataRead);
                    if(nextBuffer < Client.NUM_SEGMENT * givenHost.length){
                        Downloader b = client.downloader.get(nextBuffer % givenHost.length);
                        if(b.getDownloadState(nextBuffer / givenHost.length)){
                            FileInputStream f = new FileInputStream(bufferPath + music.split("\\.")[0] + "//" + (nextBuffer + 1));
                            add(f);
                            //Files.delete(Paths.get(bufferPath + music.split("\\.")[0] + "//" + (nextBuffer + 1)));
                            nextBuffer++;
                        }
                    }
                }
                Files.delete(Paths.get(bufferPath + music.split("\\.")[0] + "//" + 1));
                Files.delete(Paths.get(bufferPath + music.split("\\.")[0]));
                this.player.closeDataLine();
                input.close();
                newSong.close();
            }
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
            try {
                input.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e) {
            this.flag = false;
        }
    }

    public static void main(String[] args){
        Server server_w = new Server(3000);
        Server server_g = new Server(3001);
        Server server_b = new Server(3002);
        server_w.connect();
        server_g.connect();
        server_b.connect();

        String[] host = {"localhost", "localhost", "localhost"};
        int[] port = {3000, 3001, 3002};
        Client c = new Client(host, port);

        try {
            c.socketConnect();
            String[] givenHost = {"localhost", "localhost"};
            int[] givenPort = {3000, 3001};
            PlayCore spc = new StreamPlayCore("True Love.wav", 8820, c, givenHost, givenPort);
            Thread streamPlay = new Thread(spc);
            streamPlay.start();
            streamPlay.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            server_w.end();
            server_g.end();
            server_b.end();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
