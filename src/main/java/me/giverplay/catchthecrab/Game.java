package me.giverplay.catchthecrab;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Game extends Canvas implements Runnable, MouseListener, KeyListener
{
	private static final long serialVersionUID = 1L;
	
	private static final int WIDTH = 720;
	private static final int HEIGHT = 480;
	private static final int TSIZE = 16;
	
	private ArrayList<Entity> entities = new ArrayList<>();
	
	private Rectangle hole;
	private Random rand = new Random();
	private BufferedImage image;
	private BufferedImage spritesheet;
	private BufferedImage back;
	private Spawner spawner;
	private Thread thread;
	private JFrame frame;
	
	private boolean isRunning = false;
	private boolean gameOver = false;
	private boolean showGameOver = false;
	private boolean jaMorreu = false;
	private boolean clicou = false;
	
	private int score = 0;
	private int chances = 10;
	private int maxChances = 10;
	private int maxScore = 0;
	private int gameOverFrames = 0;
	private int maxGameOverFrames = 30;
	private int mx = 0;
	private int my = 0;
	
	public static void main(String[] args)
	{
		new Game();
	}
	
	public Game()
	{
		setupWindow();
		setupAssets();
		
		addMouseListener(this);
		addKeyListener(this);
		
		start();
	}
	
	public void setupWindow()
	{
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		frame = new JFrame("Game 09 - Catch The Crab");
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.add(this);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public void setupAssets()
	{
		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		
		try
		{
			spritesheet = ImageIO.read(getClass().getResource("/Spritesheet.png"));
			back = ImageIO.read(getClass().getResource("/Back.png"));
		} 
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
		
		spawner = new Spawner();
		hole = new Rectangle(WIDTH / 2 - 20, HEIGHT / 2 - 20, 40, 40);
	}
	
	public void reset()
	{
		entities.clear();
		score = 0;
		chances = 10;
		gameOver = false;
		showGameOver = false;
		gameOverFrames = 0;
		jaMorreu = false;
	}
	
	public synchronized void start()
	{
		isRunning = true;
		thread = new Thread(this, "Main Thread");
		thread.start();
	}
	
	public synchronized void stop()
	{
		isRunning = false;
		
		try
		{
			thread.join();
		} catch (InterruptedException e)
		{
			System.out.println("Interrupted");
		}
	}
	
	public BufferedImage getSprite(int x, int y, int w, int h)
	{
		return spritesheet.getSubimage(x, y, w, h);
	}
	
	@Override
	public void run()
	{
		requestFocus();
		
		long lastTime = System.nanoTime();
		long now;
		
		double updates = 60.0D;
		double delta = 0.0D;
		double update = 1000000000 / updates;
		
		while (isRunning)
		{
			now = System.nanoTime();
			delta += (now - lastTime) / update;
			lastTime = now;
			
			if (delta >= 1)
			{
				update();
				render();
				
				delta--;
			}
		}
	}
	
	private void update()
	{
		if (gameOver)
			return;
		
		for (int i = 0; i < entities.size(); i++)
		{
			entities.get(i).tick();
		}
		
		spawner.tick();
		
		if(clicou)
		{
			clicou = false;
			for (int i = 0; i < entities.size(); i++)
			{
				Entity e = entities.get(i);
				
				if(mx > e.getX() && mx < e.getX() + e.getWid() && my > e.getY() && my < e.getY() + e.getHei())
				{
					e.destroy();
				}
			}
		}		
		
		if (score > maxScore)
			maxScore = score;
		
		if (chances <= 0 && !jaMorreu)
		{
			jaMorreu = true;
			matar();
		}
	}
	
	public void matar()
	{
		gameOver = true;
	}
	
	private void render()
	{
		BufferStrategy bs = getBufferStrategy();
		
		if (bs == null)
		{
			createBufferStrategy(3);
			return;
		}
		
		Graphics smooth = bs.getDrawGraphics();
		Graphics g = image.getGraphics();
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		
		g.drawImage(back, 0, 0, WIDTH, HEIGHT, null);
		g.fillOval(WIDTH / 2 - 20, HEIGHT / 2 - 20, 40, 40);
		
		for (int i = 0; i < entities.size(); i++)
		{
			entities.get(i).render(g);
		}
		
		smooth.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
		
		renderUI(smooth);
		
		bs.show();
	}
	
	public void renderUI(Graphics g)
	{
		g.setColor(Color.white);
		g.setFont(new Font("calibri", Font.BOLD, 22));
		
		String txt = "Record: " + maxScore;
		g.drawString(txt, WIDTH - (g.getFontMetrics().stringWidth(txt) + 10), 18);
		g.drawString("Score: " + score, 5, 18);
		
		for(int i = 0; i < maxChances; i++)
		{
			g.setColor(Color.BLACK);
			g.drawRect(6 + i * (WIDTH / 10) - 2, HEIGHT - 48, WIDTH / 10 - 15, 15);
			
			if(i < chances)
			{
				g.setColor(Color.RED);
				g.fillRect(7 + i * (WIDTH / 10) - 2, HEIGHT - 47, (WIDTH / 10) - 16, 14);
			}
		}
		
		if(gameOver)
		{
			gameOverFrames++;
			
			if(gameOverFrames >= maxGameOverFrames)
			{
				gameOverFrames = 0;
				
				showGameOver = !showGameOver;
			}
			
			if(showGameOver) 
			{
				g.setColor(Color.WHITE);
				
				String txt1 = "Game Over";
				g.setFont(new Font("calibri", Font.BOLD, 32));
				g.drawString(txt1, (WIDTH - g.getFontMetrics().stringWidth(txt1)) / 2, HEIGHT / 2 - 50);
				
				String txt2 = "Aperte ENTER para reiniciar";
				g.setFont(new Font("arial", Font.BOLD, 24));
				g.drawString(txt2,  (WIDTH - g.getFontMetrics().stringWidth(txt2)) / 2, HEIGHT / 2);
			}
		}
	}
	
	public abstract class Entity
	{
		private BufferedImage sprite;
		protected double x;
		protected double y;
		private int w;
		private int h;
		private int life;
		
		public Entity(int x, int y, int w, int h, int life, BufferedImage sprite)
		{
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.sprite = sprite;
			this.life = life;
			entities.add(this);
		}
		
		public int getX()
		{
			return (int) Math.round(x);
		}
		
		public int getY()
		{
			return (int) Math.round(y);
		}
		
		public int getLife()
		{
			return life;
		}
		
		public void hit(int hit)
		{
			this.life += hit;
		}
		
		public void setX(int x)
		{
			this.x = x;
		}
		
		public void setY(int y)
		{
			this.y = y;
		}
		
		public void moveX(int move)
		{
			int xn = (int) x + move;
			
			if (!(xn <= 0 || xn >= WIDTH - TSIZE))
			{
				x = xn;
			}
		}
		
		public void moveY(int move)
		{
			this.y += move;
		}
		
		public int getWid()
		{
			return w;
		}
		
		public int getHei()
		{
			return h;
		}
		
		public void destroy()
		{
			entities.remove(this);
		}
		
		public abstract void tick();
		
		public boolean isColliding(Entity e1)
		{
			Rectangle rec = new Rectangle(getX(), getY(), getWid(), getHei());
			Rectangle rec2 = new Rectangle(e1.getX(), e1.getY(), e1.getWid(), e1.getHei());
			
			return rec.intersects(rec2);
		}
		
		public void render(Graphics g)
		{
			g.drawImage(sprite, getX(), getY(), TSIZE, TSIZE, null);
		}
	}
	
	public class Crab extends Entity
	{
		private BufferedImage[] sprites = new BufferedImage[4];
		
		private Rectangle rec;
		
		private double dx;
		private double dy;
		
		private int eframes = 0;
		private int maxEFrames = 5;
		private int anim = 0;
		
		public Crab(int x, int y)
		{
			super(x, y, TSIZE * 2, TSIZE * 2, 3, null);
			
			double angle = Math.atan2((HEIGHT / 2 - 30) - getY(), (WIDTH / 2 - 30) - getX());
			
			this.dx = Math.cos(angle);
			this.dy = Math.sin(angle);
			
			for (int i = 0; i < 64; i += TSIZE)
			{
				sprites[(int) (i / TSIZE)] = getSprite(i, 0, TSIZE, TSIZE);
			}
		}
		
		@Override
		public void tick()
		{
			x += dx;
			y += dy;
			
			rec = new Rectangle(getX(), getY(), getWid(), getHei());
			
			if(rec.intersects(hole))
			{
				sumir();
			}
		}
		
		public void sumir()
		{
			super.destroy();
			chances--;
		}
		
		@Override
		public void destroy()
		{
			new Smoke(getX(), getY(), Sound.explosionRock);
			score++;
			super.destroy();
		}
		
		@Override
		public void render(Graphics g)
		{
			eframes++;
			
			if (eframes >= maxEFrames)
			{
				eframes = 0;
				anim++;
				
				if (anim >= sprites.length)
				{
					anim = 0;
				}
			}
			
			g.drawImage(sprites[anim], getX(), getY(), getWid(), getHei(), null);
		}
	}
	
	public class Smoke extends Entity
	{
		private BufferedImage[] sprites = new BufferedImage[4];
		
		private int eframes = 0;
		private int maxEFrames = 5;
		private int anim = 0;
		
		public Smoke(int x, int y, Sound sound)
		{
			super(x, y, TSIZE * 2, TSIZE * 2, 0, null);
			
			sound.play();
			
			for (int i = 0; i < 64; i += TSIZE)
			{
				sprites[(int) (i / TSIZE)] = getSprite(i, TSIZE, TSIZE, TSIZE);
			}
		}
		
		@Override
		public void tick()
		{
			eframes++;
			
			if (eframes >= maxEFrames)
			{
				eframes = 0;
				anim++;
				
				if (anim >= sprites.length)
				{
					destroy();
				}
			}
		}
		
		@Override
		public void render(Graphics g)
		{
			g.drawImage(sprites[anim], getX(), getY(), getWid(), getHei(), null);
		}
	}
	
	public class Spawner
	{
		private int frame = 0;
		private int maxF = 100;
		
		public void tick()
		{
			frame++;
			
			if (frame >= maxF)
			{
				frame = 0;
				
				switch (rand.nextInt(4))
				{
					case 0:
						spawnTop();
						break;
						
					case 1:
						spawnBotton();
						break;
						
					case 2:
						spawnRight();
						break;
						
					case 3:
						spawnLeft();
						
					default:
						break;
				}
			}
		}
		
		private void spawnTop()
		{
			new Crab(rand.nextInt(WIDTH - 32), -32);
		}
		
		private void spawnBotton()
		{
			new Crab(rand.nextInt(WIDTH - 32), HEIGHT);
		}
		
		private void spawnLeft()
		{
			new Crab(-32, rand.nextInt(HEIGHT - 32));
		}
		
		private void spawnRight()
		{
			new Crab(WIDTH, rand.nextInt(HEIGHT - 32));
		}
	}
	
	public static class Sound
	{
		public static final Sound explosionRock = new Sound("/explosionRock.wav");
		
		private AudioClip clip;
		
		private Sound(String name)
		{
			try
			{
				clip = Applet.newAudioClip(Sound.class.getResource(name));
			} catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
		
		public void play()
		{
			try
			{
				new Thread(() -> {
					clip.play();
				}).start();
			} catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0)
	{
	}
	
	@Override
	public void mouseEntered(MouseEvent arg0)
	{
	}
	
	@Override
	public void mouseExited(MouseEvent arg0)
	{
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		mx = e.getX();
		my = e.getY();
		clicou = true;
	}
	
	@Override
	public void mouseReleased(MouseEvent arg0)
	{
	}
	
	@Override
	public void keyPressed(KeyEvent e)
	{
		if(e.getKeyCode() == KeyEvent.VK_ENTER && gameOver)
			reset();
	}
	
	@Override
	public void keyReleased(KeyEvent arg0)
	{
	}
	
	@Override
	public void keyTyped(KeyEvent arg0)
	{
	}
}
