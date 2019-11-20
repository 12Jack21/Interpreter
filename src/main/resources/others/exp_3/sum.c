// sum of 3d array with 8-unrolling
// basic test for no.11

// n must be positive even integer
// because there is no tarrant processing
int n;
scan(n);

real err = 101010101010101.01010101001;

if (n < 2){
  print(err);
}

if(n & 1 <> 0)
  print(-err);

real a[n][n][n];

int i,j,k;
k = 0;

while(k < n){
  while(j < n){
    while(i < n){
      scan(a[k][j][i]);
      i = i + 1;
    }
    i = 0;
    j = j + 1;
  }
  j = 0;
  k = k + 1;
 }

k = 0;


real totalreal = 0;
int totalint = 0;

while(k < n/2){
  int k1 = 0,k2 = 0;
  i = 0;j = 0;
  while(i < n ){
    k1 = k1 + a[k][i][j] + a[k][i][j + 1];
    j = j + 2;
    if(j == n){
      j = 0;
      i = i + 1;
    }
  }
  i = 0;j = 0;
  while(i < n ){
    k2 = k2 + a[n - k - 1][i][j] + a[n - k - 1][i + 1][j];
    j = j + 1;
    if(j == n){
      j = 0;
      i = i + 2;
    }
  }
  totalreal = totalreal + k1 + k2;
  totalint = k2 + (totalint + k1);
  k = k + 1;
 }

real division = 11111111111.1111111111111;
print(division);
print(totalint);
print(division);
print(totalint);
print(division);
