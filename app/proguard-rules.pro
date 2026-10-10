# Add project-specific ProGuard rules here as needed once real features (Room, Hilt, SQLCipher) land.

# fastexcel.reader (Excel import) optionally uses JDK's javax.xml.stream, absent on Android;
# the library already falls back without it, so R8 just needs to stop warning about it.
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.XMLReporter
-dontwarn javax.xml.stream.XMLResolver
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn javax.xml.stream.util.XMLEventAllocator
