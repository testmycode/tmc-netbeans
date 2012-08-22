# Test My Code NetBeans plugin #

This is a NetBeans plugin for the [Test My Code server](https://github.com/testmycode/tmc-server). It allows downloading, testing and submitting exercises directly from the IDE.

## Temporary build hax ##

Due to http://jira.codehaus.org/browse/MNBMODULE-138 we include a local copy of `org.codehaus.mojo:nbm-maven-plugin:3.9-SNAPSHOT`
in `nbm-maven-tmp-copy/`. Currently it must be `mvn install`'ed before the main project can build.

## Credits ##

The project started as a Software Engineering Lab project at the [University of Helsinki CS Dept.](http://cs.helsinki.fi/). The original authors of the NetBeans plugin were

- Kirsi Kaltiainen
- Timo Koivisto
- Kristian Nordman
- Jari Turpeinen

Another team wrote the [server](https://github.com/testmycode/tmc-server).

The course instructor and current maintainer of the project is Martin Pärtel ([mpartel](https://github.com/mpartel)). Other closely involved instructors were

- Matti Luukkainen ([mluukkai](https://github.com/mluukkai))
- Antti Laaksonen
- Arto Vihavainen
- Jaakko Kurhila


## License ##

[GPLv2](http://www.gnu.org/licenses/gpl-2.0.html)

