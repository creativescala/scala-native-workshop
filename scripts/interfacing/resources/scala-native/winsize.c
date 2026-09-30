#include <signal.h>
#include <sys/ioctl.h>

// TIOCGWINSZ and SIGWINCH are macros, so they only exist when C code is
// compiled. These functions make their values available to Scala.
long workshop_tiocgwinsz(void) { return TIOCGWINSZ; }
int workshop_sigwinch(void) { return SIGWINCH; }
