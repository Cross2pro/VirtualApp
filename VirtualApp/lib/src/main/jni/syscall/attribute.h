#ifndef ATTRIBUTE_H
#define ATTRIBUTE_H

#define UNUSED __attribute__((unused))
#define FORMAT(a, b, c) __attribute__ ((format (a, b, c)))
#define DONT_INSTRUMENT __attribute__((no_instrument_function))
#define PACKED __attribute__((packed))
#define WEAK   __attribute__((weak))

#endif /* ATTRIBUTE_H */
