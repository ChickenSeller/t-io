call mvn clean install

call rd ..\..\..\..\dist\examples\im-simple\client /s /q
call xcopy target\dist\tio-examples-im-simple-client-1.7.0.v20170420-RELEASE ..\..\..\..\dist\examples\im-simple\client\ /s /e /q /y

