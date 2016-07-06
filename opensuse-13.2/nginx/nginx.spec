#-%emc-cr-s-shell-v2%-
#
# Copyright (c) 2016, EMC Corporation. All Rights Reserved.
#
# This software contains the intellectual property of EMC Corporation
# or is licensed to EMC Corporation from third parties.
# This software is protected, without limitation, by copyright law and
# international treaties.
# Use of this software and the intellectual property contained therein
# is expressly limited to the terms and conditions of the License
# Agreement under which it is provided by or on behalf of EMC.
#
#-%emc-cr-e-shell-v2%-
#

Name:       nginx
Version:    1.6.2
Release:    1
Summary:    Reverse web proxy server
Vendor:     EMC
Group:      Development/Tools
Source0:    %{name}-%{version}.tar.gz
Source1:    v0.3.0.tar.gz
Source2:    v0.25.tar.gz
License:    BSD-2-Clause
URL:        http://nginx.org
BuildArch:  x86_64
BuildRequires:  pcre-devel

%description
nginx [engine x] is an HTTP and reverse proxy server, as well as a mail proxy server, written by Igor Sysoev.  It will be leveraged in the solution as the not only as a reverse proxy but for load balancing as well.  For a more detailed description please visit the official site, http://nginx.org
This release from build 13 and up built on SLES12 and thus depends on libopenssl1.

%prep
#  Remove build location in case prior build failed.
if [ -d ${RPM_BUILD_ROOT} ] ; then
	rm -rf ${RPM_BUILD_ROOT}
fi

%setup -q
%setup -D -T -a 1 -c %{_sourcedir} -n %{_sourcedir}
%setup -D -T -a 2 -c %{_sourcedir} -n %{_sourcedir}

%build
cd %{_builddir}/%{name}-%{version}
mv %{_sourcedir}/nginx_upstream_check_module-0.3.0 %{_builddir}/%{name}-%{version}/
mv %{_sourcedir}/headers-more-nginx-module-0.25 %{_builddir}/%{name}-%{version}/
patch --directory=%{_builddir}/nginx-1.6.2 -p1 < %{_builddir}/%{name}-%{version}/nginx_upstream_check_module-0.3.0/check_1.5.12+.patch
./configure --prefix=/usr --with-http_ssl_module --with-http_stub_status_module --with-ipv6 --user=root --group=root --add-module=%{_builddir}/%{name}-%{version}/headers-more-nginx-module-0.25 --add-module=%{_builddir}/%{name}-%{version}/nginx_upstream_check_module-0.3.0
make

%install
cd %{_builddir}/%{name}-%{version}
make DESTDIR=${RPM_BUILD_ROOT} install
mkdir -p ${RPM_BUILD_ROOT}/etc/%{name}
mv ${RPM_BUILD_ROOT}/usr/conf/* ${RPM_BUILD_ROOT}/etc/%{name}/
mv ${RPM_BUILD_ROOT}/usr/html ${RPM_BUILD_ROOT}/etc/%{name}/
rm -fr ${RPM_BUILD_ROOT}/usr/conf
rm -fr ${RPM_BUILD_ROOT}/usr/html

%pre
%post

%clean
rm -rf ${RPM_BUILD_ROOT}
rm -rf %{_builddir}
rm -rf %{_tmppath}

%files
%defattr(-,root,root,-)
/etc/nginx
/usr/logs
/usr/sbin/nginx

%changelog
* Mon Jul 4 2016 Rodrigo Oshiro <ApplianceDevelopmentGroup@emc.com> 0:1.6.2-1
- Initial Version to wrap nginx as an RPM
