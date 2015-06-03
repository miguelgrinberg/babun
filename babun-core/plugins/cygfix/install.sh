#!/bin/bash
set -e -f -o pipefail
source "/usr/local/etc/babun.instance"
source "$babun_tools/script.sh"

src="$babun_source/babun-core/plugins/cygfix/src"

#/bin/cp -rf /bin/mkpasswd.exe /bin/mkpasswd.exe.current
#/bin/cp -rf $src/bin/mkpasswd_1.7.29.exe /bin/mkpasswd.exe
#chmod 755 /bin/mkpasswd.exe 

#/bin/cp -rf /bin/mkgroup.exe /bin/mkgroup.exe.current
#/bin/cp -rf $src/bin/mkgroup_1.7.29.exe /bin/mkgroup.exe
#chmod 755 /bin/mkgroup.exe

if [ ! -f "/bin/vi" ]
then
 ln -s /usr/bin/vim /bin/vi
fi
