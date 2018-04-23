ssptest: main.o
	g++ main.o -o ssptest -lpthread -m32

main.o:
	g++ -c main.cpp -o main.o -std=c++11 -m32

clean:
	rm ssptest *.o
