import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/* 
 *  Problem producenta i konsumenta
 *
 *  Autor: Pawe� Rogali�ski
 *   Data: 1 listopada 2017 r.
 *   
 *   Dodał interfejs graficzny
 *   Damian Bednarz 241283
 *   29.12.2018r.
 */


abstract class  Worker extends Thread {
	
	// Metoda usypia w�tek na podany czas w milisekundach
	public static void sleep(int millis){
		try {
			Thread.sleep(millis);
			} catch (InterruptedException e) { }
	}
	
	// Metoda usypia w�tek na losowo dobrany czas z przedzia�u [min, max) milsekund
	public static void sleep(int min_millis, int max_milis){
		sleep(ThreadLocalRandom.current().nextInt(min_millis, max_milis));
	}
	
	// Unikalny identyfikator przedmiotu wyprodukowanego
	// przez producenta i zu�ytego przez konsumenta
	// Ten identyfikator jest wsp�lny dla wszystkich producent�w
	// i b�dzie zwi�kszany przy produkcji ka�dego nowego przedmiotu
	static int itemID = 0;
	
	// Minimalny i maksymalny czas produkcji przedmiotu
	public int minProducerTime = 100;
	public int maxProducerTime = 1000;
	
	// Minimalny i maksymalny czas konsumpcji (zu�ycia) przedmiotu
	public int minConsumerTime = 100;
	public int maxConsumerTime = 1000;
	
	boolean paused;
	String name;
	Buffer buffer;
	
	public void pause() {
		paused=true;
	}
	
	public void play() {
		paused=false;
	}
	
	@Override
	public abstract void run();

	public int getMinProducerTime() {
		return minProducerTime;
	}

	public void setMinProducerTime(int minProducerTime) {
		this.minProducerTime = minProducerTime;
	}

	public int getMaxProducerTime() {
		return maxProducerTime;
	}

	public void setMaxProducerTime(int maxProducerTime) {
		this.maxProducerTime = maxProducerTime;
	}

	public int getMinConsumerTime() {
		return minConsumerTime;
	}

	public void setMinConsumerTime(int minConsumerTime) {
		this.minConsumerTime = minConsumerTime;
	}

	public int getMaxConsumerTime() {
		return maxConsumerTime;
	}

	public void setMaxConsumerTime(int maxConsumerTime) {
		this.maxConsumerTime = maxConsumerTime;
	}
}


class Producer extends Worker {

	public Producer(String name , Buffer buffer){ 
		this.name = name;
		this.buffer = buffer;
	}
	
	@Override
	public void run(){ 
		int item;
		while(true){
			sleep(100);
			if(!paused) {
				item = itemID++;
				System.out.println("Producent <" + name + ">   produkuje: " + item);
				sleep(minProducerTime, maxProducerTime);
				
				// Producent umieszcza przedmiot w buforze.
				buffer.put(this, item);
			}
		}
	}
	
} // koniec klasy Producer


class Consumer extends Worker {
	
	public Consumer(String name , Buffer buffer){ 
		this.name = name;
		this.buffer = buffer;
	}

	@Override
	public void run(){ 
		int item;
		while(true){
			sleep(100);
			// Konsument pobiera przedmiot z bufora
			if(!paused) {
				item = buffer.get(this);
				
				// Konsument zu�ywa popraany przedmiot.
				sleep(minConsumerTime, maxConsumerTime);
				System.out.println("Konsument <" + name + ">       zużył: " + item+"\n");
			}
		}
	}
	
} // koniec klasy Consumer


class Buffer {
	
	private BlockingQueue<Integer> contents;
	private int size;
	private JTextArea out;
	
	public Buffer(int contents, int size, JTextArea out) {
		this.size = size;
		this.out = out;
		this.contents = new ArrayBlockingQueue<Integer>(size, true);
	}

	public synchronized int get(Consumer consumer){
		out.append("Konsument <" + consumer.name + "> chce zabrac\n");
		out.setCaretPosition(out.getDocument().getLength());
		while (contents.isEmpty()){
			try { out.append("Konsument <" + consumer.name + ">   bufor pusty - czekam\n");
				  out.setCaretPosition(out.getDocument().getLength());
				  wait();
				} catch (InterruptedException e) { }
		}
		int item=0;
		try {
			item = contents.take().intValue();
		} catch (InterruptedException e) {
			
		}
		out.append("Konsument <" + consumer.name + ">      zabral: " + item +"\n");
		out.setCaretPosition(out.getDocument().getLength());
		notifyAll();
		return item;
	}

	public synchronized void put(Producer producer, int item){
		out.append("Producent <" + producer.name + ">  chce oddac: " + item+"\n");
		out.setCaretPosition(out.getDocument().getLength());
		while (contents.size() == size){
			try { out.append("Producent <" + producer.name + ">   bufor zajety - czekam\n");
			      out.setCaretPosition(out.getDocument().getLength());
				  wait();
				} catch (InterruptedException e) { }
		}
		contents.add(item);
		out.append("Producent <" + producer.name + ">       oddal: " + item+"\n");
		out.setCaretPosition(out.getDocument().getLength());
		notifyAll();
	}
	
} // koniec klasy Buffer


public class ProducerConsumer extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private static final String APP_INSTRUCTION =
			"                  O P I S   P R O G R A M U \n\n" + 
	        "Pokazanie problemu producentów i konsumetów w formie tekstowej\n";
	private static final String APP_AUTHOR =
			"                  O   A U T O R Z E\n\n"+
			" Damian Bednarz\n\n"+
			" Data: grudzień 2018";		
	
	private boolean started=false;
	private boolean paused=false;
	private List<Producer> producers = new ArrayList<Producer>();
	private List<Consumer> consumers = new ArrayList<Consumer>();
	private JMenuBar menuBar = new JMenuBar();
	private JPanel panel = new JPanel();
	private JTextArea area = new JTextArea();
	private Buffer buffer;
	private JMenu menuFile = new JMenu("File");
	private JMenu menuHelp = new JMenu("Help");
	private JMenu menuTime = new JMenu("Time");
	private JMenuItem menuAuthor = new JMenuItem ("Author");
	private JMenuItem menuInfo = new JMenuItem ("Information");
	private JMenuItem menuEnd = new JMenuItem ("Quit");
	private JMenuItem menuRestart = new JMenuItem ("Restart");
	private JMenuItem maxPTime = new JMenuItem ("Change max producer time");
	private JMenuItem minPTime = new JMenuItem ("Change min producer time");
	private JMenuItem maxCTime = new JMenuItem ("Change max consumer time");
	private JMenuItem minCTime = new JMenuItem ("Change min consumer time");
	private JLabel labelBuffer = new JLabel("Buffer size: ");
	private JLabel labelProducer = new JLabel("Producer number: ");
	private JLabel labelConsumer = new JLabel("Consumer number: ");
	private JComboBox <Integer> comboBoxBuffer = new JComboBox(); 
	private JComboBox <Integer> comboBoxConsumer = new JComboBox(); 
	private JComboBox <Integer> comboBoxProducer = new JComboBox();
	private JButton buttonStart = new JButton("Start");
	private JButton buttonPause = new JButton("Pause");
	
	
	public ProducerConsumer () {
		super("ProducerConsumer App");
		int i;
		setResizable(false);
        setSize(650, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        menuAuthor.addActionListener(this);
        menuInfo.addActionListener(this);;
        menuEnd.addActionListener(this);
        menuRestart.addActionListener(this);
        maxPTime.addActionListener(this);
        minPTime.addActionListener(this);
        maxCTime.addActionListener(this);
        minCTime.addActionListener(this);
        menuFile.add(menuEnd);
        menuFile.add(menuRestart);
        menuHelp.add(menuAuthor);
        menuHelp.add(menuInfo);
        menuTime.add(maxPTime);
        menuTime.add(minPTime);
        menuTime.add(maxCTime);
        menuTime.add(minCTime);
        menuBar.add(menuFile);
        menuBar.add(menuHelp);
        menuBar.add(menuTime);
        for (i = 1; i < 9; ++i) {
            this.comboBoxBuffer.addItem(i);
        }
        for (i = 1; i < 8; ++i) {
            this.comboBoxProducer.addItem(i);
            this.comboBoxConsumer.addItem(i);
        }
        
        comboBoxBuffer.addActionListener(this);
        comboBoxProducer.addActionListener(this);
        comboBoxConsumer.addActionListener(this);
        buttonStart.addActionListener(this);
        buttonPause.addActionListener(this);
        setJMenuBar(menuBar);
        panel.add(labelBuffer);
        panel.add(comboBoxBuffer);
        panel.add(labelProducer);
        panel.add(comboBoxProducer);
        panel.add(labelConsumer);
        panel.add(comboBoxConsumer);
        area.setEditable(false);
        area.setFont(new Font("", 0, 12));
        area.setColumns(32);
        area.setRows(16);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setHorizontalScrollBarPolicy(31);
        panel.add(scrollPane);
        panel.add(buttonStart);
        panel.add(buttonPause);
        setContentPane(panel);
        setVisible(true);
	}
	

	public static void main(String[] args){
		new ProducerConsumer();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if(source == buttonStart) {
			if(paused==true) {
				for(Producer p : producers) {
					p.play();
				}
				for(Consumer c : consumers) {
					c.play();
				}
				paused=false;
			}
			if(started==false) {
				int i;
				buffer=new Buffer(0,(Integer)comboBoxBuffer.getSelectedItem(),area);
				for(i = 1; i<=(Integer)comboBoxProducer.getSelectedItem();i++  ) {
					producers.add(new Producer("P"+i,buffer));
				}
				for(i = 1; i<=(Integer)comboBoxConsumer.getSelectedItem();i++  ) {
					consumers.add(new Consumer("C"+i,buffer));
				}
				for(Producer p : producers) {
					p.start();
				}
				for(Consumer c : consumers) {
					c.start();
				}
				started=true;
				comboBoxBuffer.setEnabled(false);
				comboBoxProducer.setEnabled(false);
				comboBoxConsumer.setEnabled(false);
			}
		}
		if(source==buttonPause) {
			for(Producer p : producers) {
				p.pause();
			}
			for(Consumer c : consumers) {
				c.pause();
			}
			paused=true;
		}
		if(source==menuRestart) {
			dispose();
			new ProducerConsumer();
		}
		if(source==menuEnd) {
			dispose();
		}
		if(source==menuInfo) {
			JOptionPane.showMessageDialog(this,APP_INSTRUCTION);
		}
		if(source==menuAuthor) {
			JOptionPane.showMessageDialog(this, APP_AUTHOR);
		}
		if(source==maxPTime) {
			int time = Integer.parseInt(JOptionPane.showInputDialog(this,"input maximum producer time"));
			for(Producer p : producers) {
				p.setMaxProducerTime(time);
			}
		}
		if(source==minPTime) {
			int time = Integer.parseInt(JOptionPane.showInputDialog(this,"input minimum producer time"));
			for(Producer p : producers) {
				p.setMinProducerTime(time);
			}
		}
		if(source==maxCTime) {
			int time = Integer.parseInt(JOptionPane.showInputDialog(this,"input maximum consumer time"));
			for(Consumer p : consumers) {
				p.setMaxConsumerTime(time);
			}
		}
		if(source==minCTime) {
			int time = Integer.parseInt(JOptionPane.showInputDialog(this,"input minimum consumer time"));
			for(Consumer p : consumers) {
				p.setMinConsumerTime(time);
			}
		}
	}
	
} 


