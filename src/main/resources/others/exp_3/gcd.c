// greatest common divisor based on Eucidean algorithm
// basic test for no.7


int a,b;
scan(a);
scan(b);
int ba = a,bb = b;

if(b > a){
  int t = a;
  a = b;
  b = t;
 }


// keep a > b
int k = a - a / b * b;

while(k > 1){
  if(b - b / k * k == 0)
    break;
  a = b;
  b = k;
  k = a - a / b * b;
 }

// print
int gcd = k;
if(k < 1){
  print(k);
  print(0);
  print(0);
 }else {
print(gcd);
print(ba / gcd);
print(bb / gcd);
}


