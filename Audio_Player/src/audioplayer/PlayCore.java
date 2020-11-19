package audioplayer;
import javax.sound.sampled.AudioInputStream;
import java.io.*;

public class PlayCore implements Runnable {
    public InputStream input;
    public int bufferSize;
    public Boolean flag;
    public WavPlayer player;

    public PlayCore(){
        this.flag = true;
    }

    public PlayCore(String location, int bufferSize) throws IOException {
        this();
        //FileInputStream fis = new FileInputStream(location);
        /*AudioInputStream ais = WavPlayer.getAudioStream(fis,
                reader.fmt, reader.dataLength);*/
        //input.skip(100);
        this.bufferSize = bufferSize;
        this.player = new WavPlayer(location);
        input = this.player.head.getAudio();
    }

    public PlayCore(ByteArrayInputStream bis, int bufferSize, WavPlayer player) throws IOException {
        this();
        /*AudioInputStream ais = new AudioInputStream(
                reader.audio, reader.fmt, reader.dataLength);*/
        input.skip(100);
        this.bufferSize = bufferSize;
        this.player = player;
        this.input = bis;
    }

    public void end(){
        synchronized (flag){
            this.flag = false;
        }
    }

    @Override
    public void run() {
        try {
            this.player.playLock = new Object();
            synchronized (this.player.playLock) {
                this.player.playState = true;
                this.player.audioLine.start();
                byte[] data = new byte[bufferSize];

                try {
                    int dataRead = 0;
                    while (this.flag && (dataRead = input.read(data)) != -1) {
                        synchronized (this.player.restTime) {
                            if (this.player.head != null) this.player.restTime.time = this.player.audioLine.getMicrosecondPosition() / 1000000.0;
                        }
                        while (!this.player.playState) {
                            this.player.playLock.wait();
                        }
                        this.player.audioLine.write(data, 0, data.length);
                    }
                } catch (IOException e) {
                    this.flag = false;
                }
                this.player.closeDataLine();
            }

            this.input.close();
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}