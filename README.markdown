# Test My Code NetBeans plugin #

This is a NetBeans plugin for the [Test My Code server](https://github.com/testmycode/tmc-server). It allows downloading, testing and submitting exercises directly from the IDE.

## Developer setup ##

:exclamation: If you cannot find `NetBeans Platforms` from within `Tools` activate *Developing NetBeans* plugin from `Tools -> Plugins -> Installed` check the checkbox next to *Developing NetBeans* and click `Activate`. 

To ensure compatibility to a specific release, the project is configured to use a fresh unpatched installation of a specific version of NetBeans (currently 8.0.2). Download the "OS Independent Zip" edition of this version of NetBeans and extract it somewhere.
Then start your ordinary NetBeans (NOT the one you just extracted, another one). Go to `Tools -> NetBeans Platforms` and
add the extracted directory as a new platform. It should get automatically named something like "nb802".

Now open the plugin's master project in NetBeans and build it. Then right click the project and select `Open Required Projects`. If NB shows errors in the editor, try restarting NB (they should have been eliminated after the first build). Now the project should work as any other NB plugin project.

For a little more convenience while testing, you might want to set `tmc-plugin/src/fi/helsinki/cs/tmc/tailoring/SelectedTailoring.properties` to use `DeveloperTailoring`.

## Credits ##

Current maintainers of the project are
- Martin Pärtel ([mpartel](https://github.com/mpartel))
- Jarmo Isotalo ([jamox](https://github.com/jamox))

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

The system was improved and C language support was added in another SE lab project by

- Jarmo Isotalo ([jamox](https://github.com/jamox))
- Tony Kovanen ([rase-](https://github.com/rase-))
- Kalle Viiri ([Kviiri](https://github.com/Kviiri))

Checkstyle-support was integrated by

- Kenny Heinonen ([kennyhei](https://github.com/kennyhei/))
- Kasper Hirvikoski ([kasper](https://github.com/kasper/))
- Jarmo Isotalo ([jamox](https://github.com/jamox/))
- Joni Salmi ([josalmi](https://github.com/josalmi/))

## License ##

[GPLv2](http://www.gnu.org/licenses/gpl-2.0.html)

