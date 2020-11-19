package audioplayer;
import javax.sound.sampled.*;
import java.io.*;
import java.util.Scanner;

public class WavPlayer {
    WavReader head;
    SourceDataLine audioLine;
    Object playLock;
    public boolean playState;
    public FloatControl volume;
    boolean volumeFlag;
    public playTime restTime;

    public class playTime{
        public double time;
        public double totalTime;
        playTime(double totalTime){
            this.totalTime = totalTime;
            this.time = 0;
        }
    }

    public void stop(){
        audioLine.flush();
        audioLine.stop();
        synchronized (playLock) {
            if(!playState) {
                playState = true;
                playLock.notify();
            }
        }
        volume = null;
        volumeFlag = true;
        head = null;
        restTime.totalTime = -1;
        restTime.time = 0;
    }

    public WavPlayer(String location) throws FileNotFoundException {
        head = new WavReader(location);
        volume = null;
        restTime = new playTime(-1);
    }

    public WavPlayer(File data) throws FileNotFoundException {
        head = new WavReader(data);
        volume = null;
        restTime = new playTime(-1);
    }

    /*public static AudioInputStream getAudioStream(InputStream stream, AudioFormat fmt, long length){
        return new AudioInputStream(stream, fmt, length);
    }*/

    public void initialDataLine(){
        DataLine.Info inf = new DataLine.Info(SourceDataLine.class, head.fmt, head.dataLength);
        try{
            audioLine = (SourceDataLine) AudioSystem.getLine(inf);
            audioLine.open(head.fmt);
            volume = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
            volumeFlag = false;
            synchronized(restTime){
                if(restTime.totalTime == -1)
                    restTime.totalTime = head.getAudioTime(head.dataLength);
            }
        }
        catch(LineUnavailableException e){
            System.out.println("SourceDataLine is unavailable!");
        }
    }

    public void closeDataLine(){
        if(audioLine != null)
        {
            audioLine.drain();
            audioLine.close();
        }
    }

    /*public PlayCore getPlayAudio(String location, int bufferSize) throws FileNotFoundException {
        return new PlayCore(location, bufferSize, this);
    }

    public PlayCore getPlayAudio(ByteArrayInputStream bis, int bufferSize) throws IOException {
        return new PlayCore(bis, bufferSize, this);
    }*/

    /*synchronized void checkPlayState(String s){//test code, to be modified after release of GUI
        if(s.equals("p")){
            playState = false;
        } else if(s.equals("r")){
            playState = true;
            synchronized (playLock) {
                playLock.notify();
            }
        }
    }*/

    public synchronized void pause(){
        if(playState) {
            audioLine.flush();
            playState = false;
        }
    }

    public synchronized void resume(){
        if(!playState){
            playState = true;
            synchronized (playLock) {
                playLock.notify();
            }
        }
    }

    public void changeVolume(float vol){
        volumeFlag = true;
        volume.setValue(vol);
    }

    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        //loc = sc.nextLine();
        String loc = "true_love.wav";
        WavPlayer wp = new WavPlayer(loc);
        wp.initialDataLine();
        System.out.println("Max: " + wp.volume.getMaximum());
        System.out.println("Min: " + wp.volume.getMinimum());
        /*Thread play = new Thread(new WavPlayer.playAudio());
        play.start();
        while(play.isAlive()){
            float vol = sc.nextFloat();
            wp.changeVolume(vol);*/
        //}
    }
}
