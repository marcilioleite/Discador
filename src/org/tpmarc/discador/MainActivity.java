package org.tpmarc.discador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;

public class MainActivity extends Activity {

	// Constantes
	private final int TELA_SUPERIOR = 1;
	private final int TELA_INFERIOR = 2;
	
	// Variáveis
	private List<Integer> numerosDiscados = new ArrayList<Integer>();
	private Handler handler = new Handler();
	
	private boolean chamadaIniciada = false;
	private boolean toqueLiberado 	= true;
	private long	tempoToque	= 0;
	private long 	ultimoToque = 0;
	private int 	ladoTocado 	= 0;
	private int  	toquesConsecutivos = 0;
	
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
			
			if (!toqueLiberado && tempoToque > 3000) {
				
				try {
					play("audio/discando.mp3");
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				Intent intent = new Intent(Intent.ACTION_CALL);
				intent.setData(Uri.parse("tel:" + getNumeroDiscado()));
				startActivity(intent);
				
				chamadaIniciada = false;
				numerosDiscados.clear();
				toqueLiberado = true;
				tempoToque 	= 0;
				ultimoToque = 0;
				ladoTocado 	= 0;
				toquesConsecutivos = 0;
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
				if (ladoTocado == TELA_SUPERIOR) {
					
					numerosDiscados.add(toquesConsecutivos);
					
					try {
						play("audio/"+toquesConsecutivos+".mp3");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// No caso de toques no lado inferior da tela, a contagem
				// é virtualmente iniciada no número 9. Então o digito computado
				// deve ser 10 menos quantidade de toques consecutivos.
				else {
					
					numerosDiscados.add(10 - toquesConsecutivos);
					
					try {
						play("audio/"+(10 - toquesConsecutivos)+".mp3");
					} catch (IOException e) {
						e.printStackTrace();
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
			
			Log.d("Números discados: ", numerosDiscados.toString());
			
			handler.postDelayed(this, 100);
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		try {
			play("audio/pronto.mp3");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		handler.postDelayed(routine, 100);
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
				}

				/*
				 * Registra o tempo do último toque.
				 */
				ultimoToque = new Date().getTime();
				
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

	public void play(String fileName) throws IOException
	{
	    AssetFileDescriptor descriptor = getAssets().openFd(fileName);
	    long start = descriptor.getStartOffset();
	    long end = descriptor.getLength();
	    MediaPlayer mediaPlayer=new MediaPlayer();
	    mediaPlayer.setDataSource(descriptor.getFileDescriptor(), start, end);
	    mediaPlayer.prepare();
	    mediaPlayer.start();     
	}
	
	public String getNumeroDiscado() {
		String numero = "";
		for (int n : numerosDiscados) {
			numero += n;
		}
		return numero;
	}
	
}
