// insert sort
// basic test no.1

int a[5];
a[0] = 1;
for(int i = 1;i < 5;i = i + 1){
  scan(a[i]);
  int b = a[i] - 1;
  int j = i - 1;
  int i1 = i;
  while(j >= 0 && a[j] > a[i1]){
    b = a[j];
    a[j] = a[a1];
    a[i1] = b;
    i1 = j;
    j = j - 1;
  }
}

for(int i = 0; i < 5;i = i + 1)
  print(a[i]);

// print('\n');
