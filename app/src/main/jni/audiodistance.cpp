//
// Created by chentong on 2020/7/16.
//

/*#include <jni.h>

JNIEXPORT jstringJNICALL Java_cn_edu_nju_llapndk_MainActivity_HelloJNI(JNIEnv
*env,
jobject instance
)
{

// TODO


return (*env)-> NewStringUTF(env, "jnigood");
}*/

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include "jni.h"

#include <string.h>
#include <jni.h>
#include <stdio.h>
#include <math.h>

#define coscycle 960
#define cicdec   16
#define cicsec   3
#define cicdelay 33  //33 is better than 17, we use this in demo
#define cicstep  128   //32*4
#define cicnum   32
#define pi  3.141592653
#define thr 4
#define thrpeak 20
#define dctrend 0.3
#define temperature 20
#define thrmod    0.00
#define nfft  64
#define nfft_log 6
#define finter 350
#define micdis 2.0
#define idftthr  0.2


static int bufsin[16][coscycle*2];
static int bufcos[16][coscycle*2];
static double ciccomb[64][cicsec][cicdelay+5];
static double ciccum[64][cicsec];
static double wavelength[64];
static double soundspeed;
static double freqpower[64];
static double hist_freqpower[64];

static int c_numfreq=16;
static double c_wavefreqs[32];
static int curpos=0;
static int cicpos=0;
static double c_samplerate=48000;
static int now=0;
static double dinter=0;


static double dcvalue[64];
static short maxvalue[64];
static short minvalue[64];
static double phasemem[64];
static double phasememdif[64];
static double targetid;

// get the baseband through the recorded sound, output in out data;
inline double updatecicbuffer(int datain, int f, int pos){
    int i;

    ciccum[f][0]+=datain;
    ciccum[f][1]+=ciccum[f][0];
    ciccum[f][2]+=ciccum[f][1];

    if(pos!=0) //cicdec point
    {
        ciccomb[f][0][pos]=ciccum[f][2];
        ciccomb[f][1][pos]=(ciccomb[f][0][pos]-ciccomb[f][0][pos+1])/cicdelay/cicdec;
        ciccomb[f][2][pos]=(ciccomb[f][1][pos]-ciccomb[f][1][pos+1])/cicdelay/cicdec;

        if(pos==1)
        {
            for (i=0;i<cicsec;i++)
            {
                ciccomb[f][i][cicdelay+2]=ciccomb[f][i][pos];
            }
        }
        return ( ((ciccomb[f][2][pos]-ciccomb[f][2][pos+1])/cicdelay/cicdec/32768));
    }
    else
    {
        return 0.0;
    }


};

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_llap_1android_AudioDistance_getbaseband(JNIEnv *env, jobject instance, jshortArray data_,
                                                 jdoubleArray outdata_, jint numdata) {
    char result [255];
    int i,f,n,k,l;
    int c_numdata,c_cicpos,cicindex;
    int datapoint;
    double dataresult[4];
    jboolean isCopy;
    jshort *data = (*env).GetShortArrayElements( data_, &isCopy);
    jdouble *outdata = (*env).GetDoubleArrayElements(outdata_,&isCopy);

    c_numdata=numdata;
    now++;

    if(now>60000)
    {
        now=0;
        for(f=0;f<4*c_numfreq;f++)
            for(i=0;i<cicsec;i++) {
                ciccum[f][i]=0;
                for (n = 0; n < cicdelay + 5; n++)
                    ciccomb[f][i][n] = 0;
            }
    }

    for (f=0;f<c_numfreq;f++)//loop over all freqs
    {
        l=f*cicstep;
        n=0;
        c_cicpos=cicpos;

        for(i=0;i<c_numdata;i++)
        {
            if(i%cicdec==0)
                cicindex=c_cicpos;
            else
                cicindex=0;
            datapoint=((int) (data[n]))* bufcos[f][curpos+i];
            dataresult[0]=updatecicbuffer(datapoint,f,cicindex);

            datapoint=((int) (data[n]))* bufsin[f][curpos+i];
            dataresult[1]=updatecicbuffer(datapoint,f+c_numfreq,cicindex);


            datapoint=((int) (data[n+1]))* bufcos[f][curpos+i];
            dataresult[2]=updatecicbuffer(datapoint,f+c_numfreq*2,cicindex);


            datapoint=((int) (data[n+1]))* bufsin[f][curpos+i];
            dataresult[3]=updatecicbuffer(datapoint,f+c_numfreq*3,cicindex);

            if(i%cicdec==0)
            {   c_cicpos=c_cicpos+1;
                if(c_cicpos==cicdelay+2) //cic pos from 1-- cicdelay +1
                    c_cicpos=1;

                k=i/cicdec*4;


                outdata[l+k]=dataresult[0];
                outdata[l+k+1]= dataresult[1];
                outdata[l+k+2]=  dataresult[2];
                outdata[l+k+3]= dataresult[3];
            }



            n=n+2;

        }

    }

    cicpos=cicpos+c_numdata/16;
    while(cicpos>cicdelay+1)
        cicpos=cicpos-cicdelay-1;
    curpos=curpos+c_numdata;
    if(curpos>=coscycle)
    {
        curpos=curpos-coscycle;
    }
    sprintf(result,"curpos = %d",curpos);
    (*env).ReleaseShortArrayElements( data_, data, 0);
    (*env).ReleaseDoubleArrayElements(outdata_, outdata, 0);


    return (*env).NewStringUTF(result);
}








extern "C" JNIEXPORT jstring JNICALL
Java_com_example_llap_1android_AudioDistance_initdownconvert(JNIEnv *env, jobject instance, jint samplerate,
                                                     jint numfreq, jdoubleArray wavfreqs_) {
    char result[255];
    int f,i,n;
    jboolean isCopy;
    jdouble *wavfreqs = (*env).GetDoubleArrayElements(wavfreqs_, &isCopy);

    curpos=0;
    cicpos=1;


    c_numfreq=numfreq;
    for(f=0;f<c_numfreq;f++)
        c_wavefreqs[f]=wavfreqs[f];

    for(f=0;f<4*c_numfreq;f++)
        for(i=0;i<cicsec;i++) {
            ciccum[f][i]=0;
            for (n = 0; n < cicdelay + 5; n++)
                ciccomb[f][i][n] = 0;
        }
    for (f=0;f<c_numfreq;f++)
        for (n=0;n<coscycle*2;n++)
        {
            bufcos[f][n]=(int) (cos(2*pi*n/c_samplerate*c_wavefreqs[f])*32767);
            bufsin[f][n]=(int) (-sin(2*pi*n/c_samplerate*c_wavefreqs[f])*32767);
        }

    for(f=0;f<4*c_numfreq;f++)
    {
        dcvalue[f]=0;
        maxvalue[f]=0;
        minvalue[f]=0;
        hist_freqpower[f]=thr*thr*cicnum;
    }
    for(f=0;f<2*c_numfreq;f++) {
        phasemem[f] = 0;
        phasememdif[f] = 0;
    }

    now=0;
    soundspeed =331.3 + 0.606 *temperature;
    dinter=soundspeed/finter*1000.0/nfft;

    targetid= micdis/dinter;

    for(f=0;f<c_numfreq;f++)
        wavelength[f]=soundspeed/c_wavefreqs[f]*1000;


    (*env).ReleaseDoubleArrayElements(wavfreqs_, wavfreqs, 0);

    sprintf(result,"intitialize finished %d,%d",bufcos[0][0],bufcos[0][1]);
    return (*env).NewStringUTF( result);
}

extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_llap_1android_AudioDistance_removedc(JNIEnv *env, jobject instance, jdoubleArray data_,
                                              jdoubleArray data_nodc_, jshortArray outdata_) {
    char result[255];
    int f,l,k,sid,did,i,n=0;
    double init_data;
    double  vsum,dsum;
    double max_val,min_val;
    int max_id,min_id;
    jdouble *data = (*env).GetDoubleArrayElements( data_, NULL);
    jdouble *data_nodc = (*env).GetDoubleArrayElements(data_nodc_, NULL);
    jshort *outdata = (*env).GetShortArrayElements( outdata_, NULL);

    for (f=0;f<c_numfreq;f++)//loop over all freqs
    {
        l = f * cicstep;
        for (k=0;k<4;k++)
        {
            sid=f*4+k;
            vsum=0;
            dsum=0;
            init_data=data[l+k];
            max_val=init_data;
            max_id=0;
            min_val=init_data;
            min_id=0;
            n=0;
            for(i=k;i<cicstep;i+=4)
            {
                vsum=vsum+(data[l+i]-init_data)*(data[l+i]-init_data);
                dsum=dsum+data[l+i]-init_data;
                if(data[l+i]>max_val)
                {
                    max_val=data[l+i];
                    max_id=n;
                }
                if(data[l+i]<min_val)
                {
                    min_val=data[l+i];
                    min_id=n;
                }
                n++;
            }
            freqpower[sid]=vsum+dsum*dsum*cicnum;

            //freqpower[sid]=freqpower[sid]/hist_freqpower[sid];
            if(freqpower[sid]>thr*thr*cicnum)
            {
                if(max_val>maxvalue[sid] || ((max_val>minvalue[sid]+thrpeak)&&(maxvalue[sid]-minvalue[sid])>thrpeak*4&&(max_id>1)&&((cicnum-max_id)>2 ) ) )
                {
                    maxvalue[sid]=max_val;
                }
                if(min_val<minvalue[sid] || ((min_val<maxvalue[sid]-thrpeak)&&(maxvalue[sid]-minvalue[sid])>thrpeak*4&&(min_id>1)&&((cicnum-min_id)>2 ) ) )
                {
                    minvalue[sid]=min_val;
                }
                if(maxvalue[sid]-minvalue[sid]>thrpeak*2)
                    dcvalue[sid]=(1-dctrend)*dcvalue[sid]+(maxvalue[sid]+minvalue[sid])/2*dctrend;
            }
            else
            {
                //hist_freqpower[sid]=0.99*hist_freqpower[sid]+0.01*freqpower[sid]*hist_freqpower[sid];
            }
            for(i=k;i<cicstep;i+=4)
            {
                data_nodc[l+i]=data[l+i]-dcvalue[sid];
            }


        }
    }

    for(f=0;f<4*c_numfreq;f++) {
        outdata[f] = (short) dcvalue[f];
    }

    (*env).ReleaseDoubleArrayElements(data_, data, 0);
    (*env).ReleaseDoubleArrayElements(data_nodc_, data_nodc, 0);
    (*env).ReleaseShortArrayElements(outdata_, outdata, 0);

    sprintf(result,"dcremoved");
    return (*env).NewStringUTF(result);
}

extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_llap_1android_AudioDistance_getdistance(JNIEnv *env, jobject instance, jdoubleArray data_,
                                                 jdoubleArray outdata_, jdoubleArray dischange_, jdoubleArray freqpow_) {
    char result [255];


    double phase[32][cicnum];
    double var[32],varsum;
    int    ignore[32],numused;
    double initphase,sumxy,sumy,delta,deltax;
    int f,l,k,i,n,fid;
    jdouble *data = (*env).GetDoubleArrayElements( data_, NULL);
    jdouble *outdata = (*env).GetDoubleArrayElements(outdata_, NULL);
    jdouble *dischange = (*env).GetDoubleArrayElements(dischange_, NULL);
    jdouble *freqpow = (*env).GetDoubleArrayElements(freqpow_, NULL);

    deltax=c_numfreq*((cicnum-1)*cicnum*(2*cicnum-1)/6-(cicnum-1)*cicnum*(cicnum-1)/4);
    for (f=0;f<c_numfreq;f++)//loop over all freqs
    {
        l = f * cicstep;
        for (k=0;k<=2;k+=2)
        {    fid=f*2+k/2;
            freqpow[fid]=0;
            if(freqpower[f*4+k]+freqpower[f*4+k+1]>thr*thr*cicnum*thrmod)
            {
                n=0;

                for(i=k;i<cicstep;i+=4)
                {


                    phase[fid][n]=atan2( (data[l+i+1]), (data[l+i]));

                    if(n==0)
                        initphase=phase[fid][0];

                    phase[fid][n]=phase[fid][n]-initphase;

                    if(n>0)
                    {
                        while(phase[fid][n]-phase[fid][n-1]>pi)
                            phase[fid][n]=phase[fid][n]-2*pi;
                        while(phase[fid][n]-phase[fid][n-1]<-pi)
                            phase[fid][n]=phase[fid][n]+2*pi;
                    }
                    n++;
                }
                for(n=0;n<cicnum;n++)
                    phase[fid][n]=-phase[fid][n]/2/pi*wavelength[f];
                ignore[fid]=0;


            }
            else
            {
                for(n=0;n<cicnum;n++)
                    phase[fid][n]=0;
                ignore[fid]=1;
            }


        }
    }

    //data fusion
    for(k=0;k<2;k++) //channels
    {   sumxy=0;
        sumy=0;
        numused=0;
        for(f=0;f<c_numfreq;f++)
        {
            fid=f*2+k;
            if(ignore[fid])
                continue;
            numused++;
            for(n=0;n<cicnum;n++)
            {
                sumxy+=n*phase[fid][n];
                sumy+=phase[fid][n];
            }
        }
        if(numused==0)
        {
            dischange[k]=0;
            continue;
        }
        delta=(sumxy-sumy*(cicnum-1)/2.0)/deltax*c_numfreq/numused;

        varsum=0;
        for(f=0;f<c_numfreq;f++)
        {
            fid=f*2+k;
            if(ignore[fid])
                continue;
            var[fid]=0;
            for(n=0;n<cicnum;n++)
            {
                var[fid]+=(phase[fid][n]-n*delta)*(phase[fid][n]-n*delta);
            }
            varsum+=var[fid];
        }
        varsum=varsum/numused;
        for(f=0;f<c_numfreq;f++)
        {
            fid=f*2+k;
            if(ignore[fid])
                continue;
            if(var[fid]>varsum*1)
                ignore[fid]=1;
        }


        sumxy=0;
        sumy=0;
        numused=0;
        for(f=0;f<c_numfreq;f++)
        {
            fid=f*2+k;
            if(ignore[fid])
                continue;
            numused++;
            for(n=0;n<cicnum;n++)
            {
                sumxy+=n*phase[fid][n];
                sumy+=phase[fid][n];
            }
        }
        if(numused==0)
        {
            dischange[k]=0;
            continue;
        }
        delta=(sumxy-sumy*(cicnum-1)/2.0)/deltax*c_numfreq/numused;

        dischange[k]=(cicnum)*delta;
    }


    (*env).ReleaseDoubleArrayElements(data_, data, 0);
    (*env).ReleaseDoubleArrayElements(outdata_, outdata, 0);

    (*env).ReleaseDoubleArrayElements(dischange_, dischange, 0);
    (*env).ReleaseDoubleArrayElements(freqpow_, freqpow, 0);

    sprintf(result,"distace x=%f,y=%f",dischange[0],dischange[1]);
    return (*env).NewStringUTF(result);
}

/*
   This computes an in-place complex-to-complex FFT
   x and y are the real and imaginary arrays of 2^m points.
   dir =  1 gives forward transform
   dir = -1 gives reverse transform
*/
short fft(short int dir,long m,double *x,double *y)
{
    long n,i,i1,j,k,i2,l,l1,l2;
    double c1,c2,tx,ty,t1,t2,u1,u2,z;

    /* Calculate the number of points */
    n = 1;
    for (i=0;i<m;i++)
        n *= 2;

    /* Do the bit reversal */
    i2 = n >> 1;
    j = 0;
    for (i=0;i<n-1;i++) {
        if (i < j) {
            tx = x[i];
            ty = y[i];
            x[i] = x[j];
            y[i] = y[j];
            x[j] = tx;
            y[j] = ty;
        }
        k = i2;
        while (k <= j) {
            j -= k;
            k >>= 1;
        }
        j += k;
    }

    /* Compute the FFT */
    c1 = -1.0;
    c2 = 0.0;
    l2 = 1;
    for (l=0;l<m;l++) {
        l1 = l2;
        l2 <<= 1;
        u1 = 1.0;
        u2 = 0.0;
        for (j=0;j<l1;j++) {
            for (i=j;i<n;i+=l2) {
                i1 = i + l1;
                t1 = u1 * x[i1] - u2 * y[i1];
                t2 = u1 * y[i1] + u2 * x[i1];
                x[i1] = x[i] - t1;
                y[i1] = y[i] - t2;
                x[i] += t1;
                y[i] += t2;
            }
            z =  u1 * c1 - u2 * c2;
            u2 = u1 * c2 + u2 * c1;
            u1 = z;
        }
        c2 = sqrt((1.0 - c1) / 2.0);
        if (dir == 1)
            c2 = -c2;
        c1 = sqrt((1.0 + c1) / 2.0);
    }

    /* Scaling for forward transform */
    if (dir == 1) {
        for (i=0;i<n;i++) {
            x[i] /= n;
            y[i] /= n;
        }
    }

    return(0);
}

extern "C" JNIEXPORT jstring
 JNICALL
Java_com_example_llap_1android_AudioDistance_getidftdistance(JNIEnv *env, jobject instance,
                                                     jdoubleArray data_, jdoubleArray outdata_) {

    char result [255];
    double fftx[nfft];
    double ffty[nfft];
    double mag,maxmag,summag;
    int f,k,i,maxid;
    jdouble *data = (*env).GetDoubleArrayElements(data_, NULL);
    jdouble *outdata = (*env).GetDoubleArrayElements(outdata_, NULL);



    for(k=0;k<2;k++) //channels
    {
        for(i=0;i<nfft;i++)
        {
            fftx[i]=0;
            ffty[i]=0;
        }
        for(f=0;f<c_numfreq;f++)
        {
            fftx[f]=data[f*cicstep+k*2];
            ffty[f]=data[f*cicstep+k*2+1];
        }

        fft(-1,nfft_log,fftx,ffty);
        maxmag=0;
        maxid=0;
        summag=0;
        for(i=0;i<nfft;i++)
        {
            mag=fftx[i]*fftx[i]+ffty[i]*ffty[i];
            summag+=mag;
            if(mag>maxmag)
            {
                maxmag=mag;
                maxid=i;
            }

        }
        if(maxmag/summag>idftthr)
            outdata[k]=maxid*dinter;
        else
            outdata[k]=0;
    }

    sprintf(result,"distace x=%f,y=%f",outdata[0],outdata[1]);
    (*env).ReleaseDoubleArrayElements(data_, data, 0);
    (*env).ReleaseDoubleArrayElements(outdata_, outdata, 0);


    return (*env).NewStringUTF(result);
}

extern "C" JNIEXPORT jstring
 JNICALL
Java_com_example_llap_1android_AudioDistance_calibrate(JNIEnv *env, jobject instance, jdoubleArray data_) {
    char result [255];
    double fftx[nfft];
    double ffty[nfft];
    double mag,maxmag,diffdc;
    int f,k,i,maxid;
    jdouble *data = (*env).GetDoubleArrayElements(data_, NULL);


    k=0;//use the first channel
    for(i=0;i<nfft;i++)
    {
        fftx[i]=0;
        ffty[i]=0;
    }
    for(f=0;f<c_numfreq;f++)
    {
        fftx[f]=data[f*cicstep+k*2];
        ffty[f]=data[f*cicstep+k*2+1];
    }

    fft(-1,nfft_log,fftx,ffty);
    maxmag=0;
    maxid=0;
    for(i=0;i<nfft;i++)
    {
        mag=fftx[i]*fftx[i]+ffty[i]*ffty[i];
        if(mag>maxmag)
        {
            maxmag=mag;
            maxid=i;
        }

    }

    diffdc=maxid-targetid;
    if(fabs(diffdc)>0.5)
    {
        if(fabs(diffdc)<1)
            diffdc=diffdc*2;
        curpos=floor(curpos-diffdc);
        if(curpos>=coscycle)
        {
            curpos=curpos-coscycle;
        }
        if(curpos<0)
        {
            curpos=curpos+coscycle;
        }
        sprintf(result,"calibrate max at %d target %f, curpos=%d,diff %f",maxid,targetid,curpos,diffdc);
    }
    else
        sprintf(result,"calibrate OK");



    (*env).ReleaseDoubleArrayElements(data_, data, 0);


    return (*env).NewStringUTF(result);
}
