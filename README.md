# GuardianConnect

A work-in-progress framework for Android applications written in Kotlin or Java to integrate with the Guardian Connect API and establish VPN connections to the Guardian Firewall infrastructure. All lower level components are exposed but the use of high level APIs in `GRDVPNHelper` are recommended.
This framework includes everything to establish a WireGuard connection through another dependency. Instructions on how to build and integrate all required components are forthcoming (see section: Sample Application)

For more information and a direct contact visit https://guardianapp.com/company/partners/


### Integration
For easy integration it is recommended to use the Guardian Connect library published on [Sonatype](https://central.sonatype.com/artifact/com.guardianapp.connect/GuardianConnect/)

```
implementation 'com.guardianapp.connect:GuardianConnect:$GuardianConnectVersion'
```


### Sample Application
The project contains a sample application built to showcase & test functionality. It is also a reference implementation for the APIs provided by the GuardianConnect library and can be run locally on any Android device
