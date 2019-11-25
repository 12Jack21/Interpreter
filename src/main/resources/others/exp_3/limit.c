// compiler limitation test
// basic test for no.8

int ARRAY_MAX = (4096 * 4096);
int ARRAY_MIN = (1024);
int INT_SIZE = 4;
int x[ARRAY_MAX];

int stride;
for(stride = 1;stride <= ARRAY_MAX / 2;stride = stride * 2)
  print(stride * INT_SIZE);


int csize,index;
for(csize = ARRAY_MIN; csize <= ARRAY_MAX; csize=csize * 2){
  print(csize * INT_SIZE);
  for(stride = 1;stride <= csize; stride = stride * 2 ){
    for(index =0;iindex < csize;index = index + stride)
      x[index] = index + stride;
    x[index - stride] = 0;

    // timing

    // testing
    real steps = 0.0;
    int nextstep = 0;
    // while time < 20
    for(int j = 0; j < 100;j++)
    for(int i = stride;i <> 0; i = i -1){
      nextstep = 0;
      nextstep = x[nextstep];
      while(nextstep <> 0 )
        nextstep = x[nextstep];
    }
    print(0.0000000000000000000);
  }
 }
