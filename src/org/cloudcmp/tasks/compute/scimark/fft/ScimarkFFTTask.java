/*
 * Copyright (c) 2008 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 *
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */
package org.cloudcmp.tasks.compute.scimark.fft;
import java.util.Map;

import org.cloudcmp.Adaptor;
import org.cloudcmp.tasks.compute.ComputeTask;
import org.cloudcmp.tasks.compute.scimark.utils.*;

/** Computes FFT's of complex, double precision data where n is an integer power of 2.
 * This appears to be slower than the Radix2 method,
 * but the code is smaller and simpler, and it requires no extra storage.
 * <P>
 *
 * @author Bruce R. Miller bruce.miller@nist.gov,
 * @author Derived from GSL (Gnu Scientific Library),
 * @author GSL's FFT Code by Brian Gough bjg@vvv.lanl.gov
 */

  /* See {@link ComplexDoubleFFT ComplexDoubleFFT} for details of data layout.
   */

public class ScimarkFFTTask extends ComputeTask {
	public ScimarkFFTTask(Adaptor adaptor) {
		super(adaptor);
		// TODO Auto-generated constructor stub
	}
	
	public String getTaskName() {
		return "scimark.fft";
	}
	
	protected SingleComputeTask getSingleComputeTask() {
		return new SingleComputeTask();
	}
	
	protected class SingleComputeTask extends ComputeTask.SingleComputeTask {
		int id = 0;

		public  final double num_flops(int N) {
			double Nd = (double) N;
			double logN = (double) log2(N);

			return (5.0*Nd-2)*logN + 2*(Nd+1);
		}

		/** Compute Fast Fourier Transform of (complex) data, in place.*/
		public   void transform(double data[]) {
			transform_internal(data, -1); }

		/** Compute Inverse Fast Fourier Transform of (complex) data, in place.*/
		public   void inverse(double data[]) {
			transform_internal(data, +1);
			// Normalize
			int nd=data.length;
			int n =nd/2;
			double norm=1/((double) n);
			for(int i=0; i<nd; i++)
				data[i] *= norm;
		}

		/** Accuracy check on FFT of data. Make a copy of data, Compute the FFT, then
		 * the inverse and compare to the original.  Returns the rms difference.*/
		public   double test(double data[]){
			int nd = data.length;
			// Make duplicate for comparison
			double copy[] = new double[nd];
			System.arraycopy(data,0,copy,0,nd);
			// Transform & invert
			transform(data);
			inverse(data);
			// Compute RMS difference.
			double diff = 0.0;
			for(int i=0; i<nd; i++) {
				double d = data[i]-copy[i];
				diff += d*d; 
			}
			return Math.sqrt(diff/nd); 
		}

		/** Make a random array of n (complex) elements. */
		public   double[] makeRandom(int n){
			int nd = 2*n;
			double data[] = new double[nd];
			for(int i=0; i<nd; i++)
				data[i]= Math.random();
			return data; 
		}

		protected   int log2(int n){
			int log = 0;
			for(int k=1; k < n; k *= 2, log++);
			if (n != (1 << log))
				throw new Error("FFT: Data length is not a power of 2!: "+n);
			return log; 
		}

		protected   void transform_internal(double data[], int direction) {
			if (data.length == 0) return;
			int n = data.length/2;
			if (n == 1) return;         // Identity operation!
			int logn = log2(n);

			/* bit reverse the input data for decimation in time algorithm */
			bitreverse(data) ;

			/* apply fft recursion */
			/* this loop executed log2(N) times */
			for (int bit = 0, dual = 1; bit < logn; bit++, dual *= 2) {
				double w_real = 1.0;
				double w_imag = 0.0;

				double theta = 2.0 * direction * Math.PI / (2.0 * (double) dual);
				double s = Math.sin(theta);
				double t = Math.sin(theta / 2.0);
				double s2 = 2.0 * t * t;

				/* a = 0 */
				for (int b = 0; b < n; b += 2 * dual) {
					int i = 2*b ;
					int j = 2*(b + dual);

					double wd_real = data[j] ;
					double wd_imag = data[j+1] ;

					data[j]   = data[i]   - wd_real;
					data[j+1] = data[i+1] - wd_imag;
					data[i]  += wd_real;
					data[i+1]+= wd_imag;
				}

				/* a = 1 .. (dual-1) */
				for (int a = 1; a < dual; a++) {
					/* trignometric recurrence for w-> exp(i theta) w */
					{
						double tmp_real = w_real - s * w_imag - s2 * w_real;
						double tmp_imag = w_imag + s * w_real - s2 * w_imag;
						w_real = tmp_real;
						w_imag = tmp_imag;
					}
					for (int b = 0; b < n; b += 2 * dual) {
						int i = 2*(b + a);
						int j = 2*(b + a + dual);

						double z1_real = data[j];
						double z1_imag = data[j+1];

						double wd_real = w_real * z1_real - w_imag * z1_imag;
						double wd_imag = w_real * z1_imag + w_imag * z1_real;

						data[j]   = data[i]   - wd_real;
						data[j+1] = data[i+1] - wd_imag;
						data[i]  += wd_real;
						data[i+1]+= wd_imag;
					}
				}
			}
		}


		protected   void bitreverse(double data[]) {
			/* This is the Goldrader bit-reversal algorithm */
			int n=data.length/2;
			int nm1 = n-1;
			int i=0;
			int j=0;
			for (; i < nm1; i++) {

				//int ii = 2*i;
				int ii = i << 1;

				//int jj = 2*j;
				int jj = j << 1;

				//int k = n / 2 ;
				int k = n >> 1;

				if (i < j) {
					double tmp_real    = data[ii];
					double tmp_imag    = data[ii+1];
					data[ii]   = data[jj];
					data[ii+1] = data[jj+1];
					data[jj]   = tmp_real;
					data[jj+1] = tmp_imag; }

					while (k <= j) {
						//j = j - k ;
						j -= k;

						//k = k / 2 ;
						k >>= 1 ;
					}
					j += k ;
			}
		}
		
		private double x[] = new double[kernel.CURRENT_FFT_SIZE];

//		// ThreadLocal for StringBuilder used to create toString()s
//		private static final ThreadLocal <double[]> threadLocalVector =
//			new ThreadLocal<double[]> () {
//				@Override protected double[] initialValue() {
//					return new double[kernel.CURRENT_FFT_SIZE];
//				}
//			};

		public  double measureFFT(int N, double mintime, Random R) {			
			if(x.length != N){
				x = new double[N];				
			}
			// initialize FFT data as complex (N real/img pairs
			//double x[] = kernel.RandomVector(N * 2, R);
			x = kernel.RandomizeVector(x, R);
			// oldx no longer used
			//double oldx[] = kernel.NewVectorCopy(x);

			long cycles = 1;
			Stopwatch Q = new Stopwatch();
			//FFT fft = new FFT();
			Q.start();
			transform(x);	// forward transform
			inverse(x);		// backward transform
			Q.stop();

			final double EPS = 1.0e-10;
			double fftTest = test(x);
			kernel.checkResults(kernel.CURRENT_FFT_RESULT, "" + fftTest, id);
			if ( fftTest / N > EPS )
				return 0.0;
			x = null;
			//oldx = null;
			return num_flops(N)*cycles/ Q.read() * 1.0e-6;
		}
		
		public void realRun() {
			kernel.init();

			try{
				double min_time = Constants.RESOLUTION_DEFAULT;
				int FFT_size = kernel.CURRENT_FFT_SIZE;
				// run the benchmark
				double res = 0.0;
				Random R = new Random(Constants.RANDOM_SEED);
				res = measureFFT( FFT_size, min_time, R);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String [] args) {
		if (args.length < 4) {
			System.err.println("Arguments: [adaptor_name] [config_file] [num_threads] [num_runs]");
			System.exit(1);
		}
		Adaptor adaptor = Adaptor.getAdaptorByName(args[0]);
		if (adaptor == null) {
			System.err.println("Unknown adaptor");
			System.exit(1);
		}
		
		adaptor.loadConfigFromFile(args[1]);		
		ScimarkFFTTask task = new ScimarkFFTTask(adaptor);
		task.configs.put("num_threads", args[2]);
		task.configs.put("num_runs", args[3]);
		Map<String, String> results = task.run();
		ComputeTask.printResults(results);
	}
}



