JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        User.java \
        TES.java \
        ECP.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
