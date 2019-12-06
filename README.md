# Linux memory analyzer (RSS + swap)

Simple tool which analyzes `/proc/<pid>/smaps` files to gather statistics
about memory usage. It will show which processes consume how much resident and swap memory.

Example output:

```
Total Pss: 6721176 KB

Top 20 Pss

Memory Usg | PID   | Command line
-----------|-------|-------------
1247984 KB | 26394 | /home/johannes/opt/idea-IC-191.6707.61/jbr/bin/java
 831590 KB |  7823 | sbt smaps-reader
 664267 KB |  8425 | sbt photo-finish
 558044 KB | 29028 | /usr/lib/firefox/firefox
 501312 KB | 29159 | /usr/lib/firefox/firefox
 315334 KB | 29137 | /usr/lib/firefox/firefox
 305410 KB |  9274 | java
 269493 KB | 29239 | /usr/lib/firefox/firefox
 264506 KB | 29096 | /usr/lib/firefox/firefox
 205580 KB | 29184 | /usr/lib/firefox/firefox
 204432 KB | 29268 | /usr/lib/firefox/firefox
 199631 KB | 29421 | /usr/lib/firefox/firefox
 176960 KB | 29206 | /usr/lib/firefox/firefox
 163157 KB |  3189 | /usr/lib/xorg/Xorg
 156734 KB | 29311 | /usr/lib/firefox/firefox
 134806 KB |  4172 | gnome-panel
  55216 KB |  5798 | /usr/lib/gnome-terminal/gnome-terminal-server
  39593 KB |  3761 | /usr/bin/cli
  38569 KB |  3718 | nautilus-desktop
  32714 KB |  2267 | /usr/bin/gnome-shell

Top 20 Pss by cmd

Memory Usg | Command line
-----------|-------------
2861228 KB | /usr/lib/firefox/firefox
1495857 KB | sbt
1247984 KB | /home/johannes/opt/idea-IC-191.6707.61/jbr/bin/java
 305410 KB | java
 163157 KB | /usr/lib/xorg/Xorg
 134806 KB | gnome-panel
  55216 KB | /usr/lib/gnome-terminal/gnome-terminal-server
  42021 KB | -zsh
  39593 KB | /usr/bin/cli
  38569 KB | nautilus-desktop
  32714 KB | /usr/bin/gnome-shell
  30923 KB | nm-applet
  27998 KB | /usr/bin/dockerd
  20660 KB | /usr/bin/gnome-screensaver
  19541 KB | /lib/systemd/systemd-journald
  11295 KB | /usr/bin/atop
  10738 KB | /usr/bin/mongod
   9471 KB | ibus-daemon
   9428 KB | /usr/bin/containerd
   8931 KB | /usr/lib/firefox/plugin-container

Total Swap: 2198432 KB

Top 20 Swap

Memory Usg | PID   | Command line
-----------|-------|-------------
 165548 KB | 29311 | /usr/lib/firefox/firefox
 160576 KB |  3189 | /usr/lib/xorg/Xorg
 122148 KB | 29028 | /usr/lib/firefox/firefox
 118756 KB | 29268 | /usr/lib/firefox/firefox
 116204 KB | 29096 | /usr/lib/firefox/firefox
 101976 KB | 29159 | /usr/lib/firefox/firefox
  94400 KB | 29239 | /usr/lib/firefox/firefox
  87760 KB | 29184 | /usr/lib/firefox/firefox
  80672 KB | 26394 | /home/johannes/opt/idea-IC-191.6707.61/jbr/bin/java
  74416 KB |  4224 | /usr/bin/gnome-screensaver
  71808 KB | 29421 | /usr/lib/firefox/firefox
  67828 KB |  2267 | /usr/bin/gnome-shell
  66684 KB |  3718 | nautilus-desktop
  59932 KB | 29137 | /usr/lib/firefox/firefox
  51296 KB | 29550 | keepassxc
  46124 KB | 29206 | /usr/lib/firefox/firefox
  40044 KB |  3765 | /usr/lib/evolution/evolution-calendar-factory
  39604 KB |  3845 | /usr/lib/evolution/evolution-calendar-factory-subprocess
  37236 KB |  5798 | /usr/lib/gnome-terminal/gnome-terminal-server
  36008 KB | 24588 | /usr/bin/dockerd

Top 20 Swap by cmd

Memory Usg | Command line
-----------|-------------
 992360 KB | /usr/lib/firefox/firefox
 160576 KB | /usr/lib/xorg/Xorg
  80672 KB | /home/johannes/opt/idea-IC-191.6707.61/jbr/bin/java
  74416 KB | /usr/bin/gnome-screensaver
  67828 KB | /usr/bin/gnome-shell
  66684 KB | nautilus-desktop
  51296 KB | keepassxc
  40044 KB | /usr/lib/evolution/evolution-calendar-factory
  39604 KB | /usr/lib/evolution/evolution-calendar-factory-subprocess
  37236 KB | /usr/lib/gnome-terminal/gnome-terminal-server
  36008 KB | /usr/bin/dockerd
  33404 KB | /lib/systemd/systemd-resolved
  32924 KB | /usr/bin/mongod
  31536 KB | gnome-panel
  29740 KB | /usr/bin/python3
  19288 KB | /usr/sbin/rpc.mountd
  16500 KB | /usr/bin/cli
  16128 KB | -zsh
  14944 KB | /usr/bin/containerd
  13516 KB | /usr/bin/Xwayland

```