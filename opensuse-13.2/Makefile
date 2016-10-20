#
# Copyright 2016 EMC Corporation
# All Rights Reserved
#

##################################################
#  Setup
##################################################
.DEFAULT: all
.PHONY: clobber createRPM downloadISO downloadRPM createREPO
all: createREPO clobber

##################################################
#  Parameters
##################################################
JOB := 999
WORKSPACE := $(PWD)
ZYPPER_PARAMS := --root $(WORKSPACE)/zypper --non-interactive
ISO_FILE := openSUSE-13.2-DVD-x86_64.iso
ISO_URL := http://download.opensuse.org/distribution/13.2/iso/$(ISO_FILE)
PACKAGES := GeoIP GeoIP-data SuSEfirewall2 ant apache2-mod_perl apache2-prefork atop bind-libs bind-utils boost-license1_58_0 ca-certificates-cacert ca-certificates-mozilla createrepo curl dhcpcd docker docker-compose expect fbiterm fontconfig fonts-config fribidi gcc-c++ git git-core glib2-devel gpgme grub2 grub2-i386-pc grub2-x86_64-efi ifplugd inst-source-utils iproute2 iputils java-1_7_0-openjdk java-1_7_0-openjdk-devel java-1_8_0-openjdk java-1_8_0-openjdk-devel keepalived kernel-default kernel-default-devel kernel-source kiwi kiwi-desc-isoboot kiwi-desc-oemboot kiwi-desc-vmxboot kiwi-templates libGeoIP1 libSDL-1_2-0 libXmu6 libaudiofile1 libboost_system1_58_0 libboost_thread1_58_0 libesd0 libgcrypt-devel libgpg-error-devel libmng2 libopenssl-devel libpcrecpp0 libpcreposix0 libqt4 libqt4-sql libqt4-x11 librados2 librbd1 libserf-devel libtool libuuid-devel libvpx1 libxml2-devel lvm2 make mkfontdir mkfontscale mozilla-nss-certs ndisc6 net-tools netcfg nfs-client openssh openssh-fips p7zip pam-devel parted pcre-devel perl-Config-General perl-Error perl-Tk plymouth python-cjson python-devel python-gpgme python-iniparse python-libxml2 python-py python-requests python-setools qemu qemu-tools readline-devel regexp rpm-build setools-libs shim sipcalc sshpass strongswan strongswan-ipsec strongswan-libs0 subversion sudo sysconfig sysconfig-netconfig syslinux sysstat systemd-logger tar telnet unixODBC vim virtualbox virtualbox-host-kmp-default wget xbitmaps xfsprogs xml-commons-jaxp-1.3-apis xmlstarlet xorg-x11-essentials xorg-x11-fonts xorg-x11-server xz-devel zlib-devel
RPMBUILD_SOURCEDIR := $(WORKSPACE)/nginx
RPMBUILD_BUILDROOT := $(WORKSPACE)/nginx/BUILDROOT
RPMBUILD_BUILDDIR := $(WORKSPACE)/nginx/BUILDDIR
RPMBUILD_TMP_DIR := $(WORKSPACE)/nginx/TMP
RPMBUILD_RPMS_DIR := $(WORKSPACE)/nginx/RPMS
RPMBUILD_FLAGS := -bb \
	--buildroot "$(RPMBUILD_BUILDROOT)" \
	--define "__spec_build_pre /bin/true" \
	--define "_topdir $(RPMBUILD_BUILDDIR)" \
	--define "_builddir $(RPMBUILD_BUILDDIR)" \
	--define "_tmppath $(RPMBUILD_TMP_DIR)" \
	--define "_sourcedir $(RPMBUILD_SOURCEDIR)" \
	--define "_rpmdir $(RPMBUILD_RPMS_DIR)"

##################################################
#  Functions
##################################################
define targetLog
	@echo "###############################################";
	@echo "$(1)[$@] on $$( date )";
	@echo "###############################################";
endef

##################################################
#  Targets
##################################################
downloadISO:
	$(call targetLog,+)
	mkdir -p $(WORKSPACE)/iso/mount
	[ ! -f /disks/adgbuild/OPENSUSE13.2/$(ISO_FILE) ] || ln -fs /disks/adgbuild/OPENSUSE13.2/$(ISO_FILE) $(WORKSPACE)/iso/
	[ -s $(WORKSPACE)/iso/$(ISO_FILE) ] || wget $(ISO_URL) --continue --progress=bar --output-document=$(WORKSPACE)/iso/$(ISO_FILE)
	$(call targetLog,-)

downloadRPM:
	$(call targetLog,+)
	# Cleanup any folder from the workspace related to zypper and initialize the database
	rm -fr $(WORKSPACE)/zypper
	rpm --initdb --root=$(WORKSPACE)/zypper

	# Mount the ISO with the package from the distribution
	-mountpoint -q $(WORKSPACE)/iso/mount || mount $(WORKSPACE)/iso/$(ISO_FILE) $(WORKSPACE)/iso/mount

	# Add the repositories that will be used to retrieve packages from external locations
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-iso --no-gpgcheck $(WORKSPACE)/iso/mount suse-13.2-iso
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-oss --no-gpgcheck http://download.opensuse.org/distribution/13.2/repo/oss/suse suse-13.2-oss
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-oss-update --no-gpgcheck http://download.opensuse.org/repositories/openSUSE:/13.2:/Update/standard suse-13.2-oss-update
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-appliances --no-gpgcheck http://download.opensuse.org/repositories/Virtualization:/Appliances/openSUSE_13.2 suse-13.2-appliances
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-containers --no-gpgcheck http://download.opensuse.org/repositories/Virtualization:/containers/openSUSE_13.2 suse-13.2-containers
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-electronics --no-gpgcheck http://download.opensuse.org/repositories/electronics/openSUSE_13.2 suse-13.2-electronics
	zypper $(ZYPPER_PARAMS) addrepo --name suse-42.1-filesystems-ceph --no-gpgcheck http://download.opensuse.org/repositories/filesystems:/ceph/openSUSE_Leap_42.1 suse-42.1-filesystems-ceph
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-seife --no-gpgcheck http://download.opensuse.org/repositories/home:/seife:/testing/openSUSE_13.2 suse-13.2-seife
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-monitoring --no-gpgcheck http://download.opensuse.org/repositories/server:/monitoring/openSUSE_13.2 suse-13.2-monitoring
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-http --no-gpgcheck http://download.opensuse.org/repositories/server:/http/openSUSE_13.2 suse-13.2-http
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-network --no-gpgcheck http://download.opensuse.org/repositories/network:/utilities/openSUSE_13.2 suse-13.2-network
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-python --no-gpgcheck http://download.opensuse.org/repositories/devel:/languages:/python/openSUSE_13.2 suse-13.2-python

	# Define the priorities of the repositories
	zypper $(ZYPPER_PARAMS) modifyrepo --priority 99 suse-13.2-iso
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  4 suse-13.2-oss
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  4 suse-13.2-oss-update
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-13.2-appliances
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-13.2-containers
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-13.2-electronics
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-42.1-filesystems-ceph
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-13.2-seife
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-13.2-monitoring
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  3 suse-13.2-http
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  5 suse-13.2-network
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  5 suse-13.2-python
	zypper $(ZYPPER_PARAMS) refresh

	# Download packages
	zypper $(ZYPPER_PARAMS) download $(PACKAGES)

	# Create a local repositories defining the downloaded packages with the highest priorities and the ISO as the second
	rm -fr $(WORKSPACE)/zypper/repo
	mkdir -p $(WORKSPACE)/zypper/repo
	find $(WORKSPACE)/zypper/var/cache/zypp/packages -iname \*.rpm -exec cp -f {} $(WORKSPACE)/zypper/repo/ \;
	createrepo $(WORKSPACE)/zypper/repo
	zypper $(ZYPPER_PARAMS) addrepo --name suse-13.2-local --no-gpgcheck $(WORKSPACE)/zypper/repo suse-13.2-local
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  1 suse-13.2-local
	zypper $(ZYPPER_PARAMS) modifyrepo --priority  2 suse-13.2-iso

	# Resolve the dependencies of the downloaded packages, downloading more packages from external locations when needed
	zypper $(ZYPPER_PARAMS) install --no-recommends --force-resolution --download-only $(PACKAGES)
	zypper $(ZYPPER_PARAMS) removerepo suse-13.2-local

	# Update the database with all the downloaded packages in order to retrieve security updates
	rm -fr $(WORKSPACE)/zypper/repo
	mkdir -p $(WORKSPACE)/zypper/repo
	find $(WORKSPACE)/zypper/var/cache/zypp/packages -iname \*.rpm -exec cp -f {} $(WORKSPACE)/zypper/repo/ \;
	rpm --root $(WORKSPACE)/zypper --install --justdb --nodeps --nodigest --nosignature --noscripts $(WORKSPACE)/zypper/repo/*.rpm

	# Retrieve the security updates
	zypper $(ZYPPER_PARAMS) --non-interactive-include-reboot-patches patch -g security --no-recommends --download-only || /bin/true
	-umount $(WORKSPACE)/iso/mount
	$(call targetLog,-)

createRPM:
	$(call targetLog,+)
	wget http://nginx.org/download/nginx-1.6.2.tar.gz --continue --progress=bar --output-document=$(WORKSPACE)/nginx/nginx-1.6.2.tar.gz
	wget --no-check-certificate https://github.com/yaoweibin/nginx_upstream_check_module/archive/v0.3.0.tar.gz --continue --progress=bar --output-document=$(WORKSPACE)/nginx/v0.3.0.tar.gz
	wget --no-check-certificate https://github.com/openresty/headers-more-nginx-module/archive/v0.25.tar.gz --continue --progress=bar --output-document=$(WORKSPACE)/nginx/v0.25.tar.gz
	rpmbuild $(RPMBUILD_FLAGS) $(WORKSPACE)/nginx/nginx.spec
	$(call targetLog,-)

createREPO: createRPM downloadISO downloadRPM
	$(call targetLog,+)
	# Build the local repository with external packages downloaded to a local workspace
	rm -fr $(WORKSPACE)/zypper/var/cache/zypp/packages/suse-13.2-iso
	rm -fr $(WORKSPACE)/zypper/repo
	mkdir -p $(WORKSPACE)/zypper/repo
	cp $(WORKSPACE)/nginx/RPMS/x86_64/nginx-*.x86_64.rpm $(WORKSPACE)/zypper/repo/
	find $(WORKSPACE)/zypper/var/cache/zypp/packages -iname \*.rpm -exec cp -f {} $(WORKSPACE)/zypper/repo/ \;
	createrepo $(WORKSPACE)/zypper/repo
	rm -fr /disks/adgbuild/OPENSUSE13.2/repo
	mv $(WORKSPACE)/zypper/repo /disks/adgbuild/OPENSUSE13.2/
	$(call targetLog,-)

clobber:
	@rm -fr $(WORKSPACE)/iso
	@rm -fr $(WORKSPACE)/zypper
	@rm -fr $(WORKSPACE)/nginx/RPM
	@rm -fr $(WORKSPACE)/nginx/*.gz
