CC	:= gcc
CXX	:= g++
LD	:= g++

CFLAGS	:=
CXXFLAGS	:= -DHOST -g -Wformat=0 -D_DEBUG_ 
LDFLAGS	:= -pthread 
INCS	:=
DEFINES :=
EXTRA_LIBS :=

ELF_CLASS_32 = YES
#ELF_CLASS_32 = NO

ifeq ($(ELF_CLASS_32), YES)
	DEFINES += -DELFCLASS32
	CFLAGS += -m32
	CXXFLAGS += -m32
	LDFLAGS += -m32
endif



SRCS := \
	main.cpp fileCoder1.cpp EncryptFile.cpp originalInterface.cpp ff_Recognizer.cpp TemplateFile.cpp \
	virtualFileSystem.cpp IgnoreFile.cpp encryptInfoMgr.cpp \
	ctr/caesar_cipher.cpp ctr/crypter.cpp ctr/ctr.cpp ctr/rng.cpp ctr/SpookyV2.cpp ctr/util.cpp

OBJS := $(addsuffix .o,$(basename ${SRCS}))
OUTPUT := e_test

all:${OUTPUT}

${OUTPUT}: ${OBJS}
	${LD} ${OBJS} ${LIBS} ${EXTRA_LIBS} ${LDFLAGS} -o $@ 

.INTERMEDIATE: ${OBJS}

clean:
	rm -f core ${OBJS} ${OUTPUT}

%.o: %.cpp
	${CXX} -c ${CXXFLAGS} ${DEFINES} ${INCS} $< -o $@

%.o: %.c
	${CC} -c ${CFLAGS} ${DEFINES} ${INCS} $< -o $@

