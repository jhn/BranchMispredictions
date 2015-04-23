default: optimizer

optimizer:
	javac src/main/java/*.java

all: clean optimizer

clean:
	rm -f src/main/java/*.class
