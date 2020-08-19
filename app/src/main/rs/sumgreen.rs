#pragma version(1)
#pragma rs java_package_name(com.example.llap_android)
#pragma rs_fp_relaxed
#define RS_KERNEL __attribute__((kernel))
uint RS_KERNEL extractGreenChannel(uchar4 rgba)
{
    return (uint)(rgba[1]);
}
#pragma rs reduce(sumGreen) \
            accumulator(sumGreenAccumulator) \
            combiner(sumGreenCombiner)

static void sumGreenAccumulator(long *accumul,uint in)
{
    *accumul += in;
}
static void sumGreenCombiner(long *val,const long *val2)
{
    *val += *val2;
}