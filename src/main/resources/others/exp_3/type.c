// type cast
// baisc test for no.1

// no char
// implicitly cast
real b = -1234567890123456;
int a = b; 
print(a);
b = -b;
a = b;
print(a);
a = -b;
b = a;
print(b); // warning signed extension 

// bit operation involved
real b1 = 0;
b1 = ~b1;
print(b1);
int a1 = 3;
int a11 = 0;
a11 = a1 * 4 | a1 & -1;
a11 = a11 * 4 | a1;
a11 = a11 * 4 | a1 & ~0;
a11 = a11 * 4 | a1;
print(a11);
a1 = a11 - (a11 / 3) * 3; // 0
print(a1);
print(a11 - (a11 + 1) / 3 * 3); // 1


// operation test
int a2 = 123456789;
real b2 = a2;
a2 = b2 * a2; // must throw overflow exception!
b2 = a2 / 0.0; // must throw divided by zero exception !

//operation cast
int a4 = 1001;
real b4 = a4 / 1000;
print(b4);
a4 = a4 * 000.100;
print(a4);



// ----------------------------------------
// CHAR & STRING
// very important and very appausible !
// --------------------------------------
char c = 97;
print(c);
print(c + 1); // is number !
print((char) c + 1);  // is char !
print((char)c * 2 - 140); // is number !
print(c + 'Z'- 'a' );
print((char)c + 31 ); // extended ascii !

// string involved
real b3 = 0.1;
char s[5] = { 'H','e','l','l','o'};
print(s);
s[0] = s[0] * 4 / 50 + 2.5 ; // ‘\n’ = 10 = 0x0A = line feed
s[1] = s[1] - 'e' + 8; // backspace
s[2] = (char)(10 / b3); // 100 = 'd'
s[3] = '\0';
s[4] = '~';
print(s);
print(s[4]);

char c = 97;print("qwewqe" + (12.9012 - 10));