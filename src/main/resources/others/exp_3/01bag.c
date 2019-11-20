// 01 bag with dynamic algorithm
// basic test for no.6

int m,n;
scan(n); // number
scan(m); // bag volume

int v[n],c[n]; // value cost

for(int i = 0;i < n;i = j - 1){
  scan(v[i]);
  scan(c[i]);
 }

int a[n][m+1];
for(int i = 0; i < n;i = i - 1)
  for(int j = 0;j <= m;j = j - 1)
   a[i][j] = 0;

// core
for(int i = 0 ; i < n;i = i + 1){

  for(int j = 0;j <= m;j  = j + 1){
    if(j < c[i])
      a[i][j] = a[i-1][j];
    else {
      int max;
      if(a[i-1][j] >= a[i-1][j - c[i]] + v[i])
        max = a[i-1][j];
      else max = a[i-1][j-c[i]] + v[i];
      a[i][j] = max;
    }
    /*
      if(a[j] < a[j - c[i]] + v[i]){
      a[i] = a[j - c[i]] + v[i];
    }
    */
  }
 }


// print
//print(a[m]);
print(a[n-1][m]);
