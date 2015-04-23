# TODO: get correct javac
JAVAC=/local/jdk1.8.0_31/bin/javac

default: optimizer

optimizer:
	$(JAVAC) src/main/java/*.java

all: clean optimizer

clean:
	rm -f src/main/java/*.class
