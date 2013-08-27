package org.tpmarc.discador;	

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

	// Constantes
	private final int TELA_SUPERIOR = 1;
	private final int TELA_INFERIOR = 2;
	
	// Variáveis
	private List<Integer> numerosDiscados = new ArrayList<Integer>();
	private Handler handler = new Handler();
	
	private SensorManager sensorManager;
	private long ultimoUpdateAcelerometro = -1;
	private float x, y, z, lastX, lastY, lastZ;
	private static final int SHAKE_THRESHOLD = 200;
	private static final int LARGE_SHAKE_THRESHOLD = 800;
	
	private boolean chamadaIniciada = false;
	private boolean toqueLiberado 	= true;
	private long	tempoToque	= 0;
	private long 	ultimoToque = 0;
	private long	ultimoShake = 0;
	private int 	ladoTocado 	= 0;
	private int  	toquesConsecutivos = 0;
	
	private TextView textViewNumeros;
	
	/*
	 * Tarefa programada executada a cada 100 milisegundos.
	 * 
	 * Se a tela já foi tocada e permanecer sem receber toques 
	 * por 2 segundos, é computada a discagem de um dígito 
	 * referente a quantidade de toques consecutivos.
	 * 
	 * 	Ex.: O usuário toca a tela duas vezes, e passa dois segundos
	 *       sem tocá-la novamente. Então o digito 2 é discado.
	 * 
	 */
	private final Runnable routine = new Runnable() {

		@Override
		public void run() {
			
			long agora = new Date().getTime();
			
			if (!chamadaIniciada && !toqueLiberado && tempoToque > 3000) {
				
				/*
				 * Se algum dígito foi discado, pode tentar chamada.
				 * 
				 */
				if ( numerosDiscados.size() > 0 ) {
					try {
						play("audio/discando.mp3");
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					Intent intent = new Intent(Intent.ACTION_CALL);
					Log.d("numero discado:", getNumeroDiscado());
					intent.setData(Uri.parse("tel:" + getNumeroDiscado()));
					startActivity(intent);
					
					chamadaIniciada = true;	
				}
				
			}
			
			if (!toqueLiberado) {
				
				tempoToque += 100;
			}
			
			/*
			 * Se a tela já foi tocada (pela primeira vez ou depois
			 * de um número ser discado) e se o último toque aconteceu
			 * há mais de 2 segundos. 
			 * 
			 */
			if (ultimoToque != 0 && agora - ultimoToque > 2000) {
				
				/*
				 * Registra a quantidade de toques consecutivos como um
				 * número discado.
				 * 
				 */
				
				// No caso de toques no lado superior da tela, a contagem
				// é virtualmente iniciada a partir do número 0. Então o digito computado
				// deve ser a quantidade de toques consecutivos menos 1.
				if (ladoTocado == TELA_SUPERIOR) {
					
					/*
					 * Se o usuário não estiver pressionando o toque na tela, mas sim
					 * discando um número... 
					 */
					if (tempoToque < 3000) {
						
						int numero = Math.min(toquesConsecutivos - 1, 9);
						
						numerosDiscados.add(numero);
						
						textViewNumeros.setText(getNumeroDiscado());
						
						try {
							play("audio/"+numero+".mp3");
						} catch (IOException e) {
							e.printStackTrace();
						}						
					}
				}
				// No caso de toques no lado inferior da tela, a contagem
				// é virtualmente iniciada no número 9. Então o digito computado
				// deve ser 10 menos quantidade de toques consecutivos.
				else {
					
					/*
					 * Se o usuário não estiver pressionando o toque na tela, mas sim
					 * discando um número... 
					 */
					if (tempoToque < 3000) {
						
						int numero = Math.max(10 - toquesConsecutivos, 0);
						
						numerosDiscados.add(numero);
						
						textViewNumeros.setText(getNumeroDiscado());
						
						try {
							play("audio/"+numero+".mp3");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				
				/* 
				 * Zera contadores e flags.
				 *  
				 */
				ultimoToque = 0;
				ladoTocado = 0;
				toquesConsecutivos = 0;
			}
			
			handler.postDelayed(this, 100);
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textViewNumeros = (TextView)findViewById(R.id.textViewNumeros);
		
		// Uso do acelerômetro.
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		boolean acelerometro = sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		
		if (!acelerometro) {
			
			sensorManager.unregisterListener(this);
		}
		
		handler.postDelayed(routine, 100);
	}
	
	/*
	 * Quando o aplicativo iniciar.
	 * 
	 */
	@Override
	protected void onStart() {
		
		super.onStart();
		
		chamadaIniciada = false;
		numerosDiscados.clear();
		textViewNumeros.setText(getNumeroDiscado());
		toqueLiberado = true;
		tempoToque 	= 0;
		ultimoToque = 0;
		ladoTocado 	= 0;
		toquesConsecutivos = 0;
		
		try {
			play("audio/pronto.mp3");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	protected void onDestroy() {

		try {
			play("audio/tchau.mp3");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		super.onDestroy();
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		if (!chamadaIniciada) {

			Display tela = getWindowManager().getDefaultDisplay();
			
			/*
			 * Ao tocar a tela.
			 * 
			 */
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				
				
				/*
				 * Altera o flag de toque liberado para false.
				 */
				toqueLiberado = false;
			}
			
			/*
			 * Ao levantar o dedo no toque.
			 * 
			 */
			if (event.getAction() == MotionEvent.ACTION_UP) {
				
				/*
				 * Altera o flag de toque liberado para true.
				 */
				toqueLiberado = true;
				
				/*
				 * Se o toque aconteceu na parte superior da tela...
				 * 
				 */
				if (event.getY() < tela.getHeight()/2) {
					
					/*
					 * Se nenhum lado foi tocado desde a abertura do programa
					 * ou desde o último digito discado, seleciona o lado superior
					 * como lado lado tocado. Só serão aceitos toques neste lado
					 * até que o usuário pare de tocar a tela para que a discagem
					 * do digito seja computada.
					 * 
					 */
					if (ladoTocado == 0) {
						
						ladoTocado = TELA_SUPERIOR;
					}

					/*
					 * Se o lado tocado não foi o mesmo do lado superior, apenas
					 * retorna false no evento de toque.
					 * 
					 */
					else if (ladoTocado != TELA_SUPERIOR) {
						
						return false;
					}
					
					/*
					 * Computa o toque.
					 */
					toquesConsecutivos++;
					/*
					 * Reseta o contador de tempo de toque.
					 */
					tempoToque = 0;
				}
				/*
				 * Se aconteceu no lado inferior da tela...
				 * 
				 */
				else {
					
					/*
					 * Se nenhum lado foi tocado desde a abertura do programa
					 * ou desde o último digito discado, seleciona o lado inferior
					 * como lado lado tocado. Só serão aceitos toques neste lado
					 * até que o usuário pare de tocar a tela para que a discagem
					 * do digito seja computada.
					 * 
					 */
					if (ladoTocado == 0) {
						
						ladoTocado = TELA_INFERIOR;
					}

					/*
					 * Se o lado tocado não foi o mesmo do lado inferior, apenas
					 * retorna false no evento de toque.
					 * 
					 */
					else if (ladoTocado != TELA_INFERIOR) {
						
						return false;
					}
					
					/*
					 * Computa o toque.
					 * 
					 */
					toquesConsecutivos++;
					/*
					 * Reseta o contador de tempo de toque.
					 */
					tempoToque = 0;
				}

				/*
				 * Registra o tempo do último toque.
				 */
				ultimoToque = new Date().getTime();
				
				try {
					play("audio/keynote.mp3");
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}		
			
		}
		
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*
	 * Toca um som da pasta assets.
	 * 
	 */
	public void play(String fileName) throws IOException {
		
	    AssetFileDescriptor descriptor = getAssets().openFd(fileName);
	    
	    long start = descriptor.getStartOffset();
	    
	    long end = descriptor.getLength();
	    
	    MediaPlayer mediaPlayer=new MediaPlayer();
	    
	    mediaPlayer.setDataSource(descriptor.getFileDescriptor(), start, end);
	    
	    mediaPlayer.prepare();
	    
	    mediaPlayer.start();  
	    
	    mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.release();
			}
		});
	}

	/*
	 * Retorna a lista de números discados como String. 
	 * 
	 */
	public String getNumeroDiscado() {
		
		String numero = "";
		
		for (int n : numerosDiscados) {
			
			numero += n;
		}
		
		return numero;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	/*
	 * Sensor de Acelerômetro que identifica o telefone sendo chacoalhado.
	 * 
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {

		Sensor sensor = event.sensor;
		
		float[] values = event.values;
		
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			
			long tempoAgora = System.currentTimeMillis();
			
			long diferencaTempo = tempoAgora - ultimoUpdateAcelerometro;
			
			if (diferencaTempo > 100) {
				
				ultimoUpdateAcelerometro = tempoAgora;
				
				x = values[SensorManager.DATA_X];
				y = values[SensorManager.DATA_Y];
				z = values[SensorManager.DATA_Z];
				
				float velocidade = Math.abs(x+y+z - lastX-lastY-lastZ)/diferencaTempo*10000;
				
				
				
				if (ultimoShake == 0 || tempoAgora - ultimoShake > 2000) {
					
					
					
					/*
					 * Se a aceleração do celular na hora que chacoalhado for
					 * muito alta.
					 * 
					 * 
					 */
					if (velocidade > LARGE_SHAKE_THRESHOLD) {
						
						try {
							play("audio/todos.mp3");
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						/*
						 * Apaga todos os números discados.
						 */
						numerosDiscados.clear();
						
						textViewNumeros.setText(getNumeroDiscado());
					}
					
					/*
					 * Se for chacoalhado em velocidade reduzida.
					 * 
					 */
					else if (velocidade > SHAKE_THRESHOLD) {
						
						/*
						 * O último número discado é cancelado.
						 */
						if (numerosDiscados.size() > 0)	{
							
							try {
								play("audio/ultimo.mp3");
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							numerosDiscados.remove(numerosDiscados.size()-1);
							
							textViewNumeros.setText(getNumeroDiscado());
						}
					}
				
					ultimoShake = tempoAgora;
				}
				lastX = x;
				lastY = y;
				lastZ = z;
			}
			
		}
		
	}
	
}
