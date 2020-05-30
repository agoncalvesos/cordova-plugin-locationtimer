//
//  LocationTimer.swift
//
//

import Foundation
import AudioToolbox
import WebKit
import CoreLocation
import UserNotifications

func log(_ message: String){
    NSLog(">>> LocationTimer - \(message)")
}

@objc(LocationTimer) class LocationTimer : CDVPlugin {
    var pluginResult: CDVPluginResult?
    var command: CDVInvokedUrlCommand?
    
    @objc(initialize:) func initialize(_ command: CDVInvokedUrlCommand) {
        log(">>> Plugin initialization <<<")
        
        self.command = command;
        
        //trackedUUID
        let trackedUUID = command.arguments[0] as? String
        if trackedUUID == nil || (trackedUUID?.count ?? 0) == 0 {
            cordovaResultError(errorMessage: "trackedUUID input parameter cannot be empty")
        } else {
            print("> TrackedUUID: \(trackedUUID ?? "")")
        }
        
        //postURL
        let postURL = command.arguments[1] as? String
        if postURL == nil || (postURL?.count ?? 0) == 0 {
            cordovaResultError(errorMessage: "postURL input parameter cannot be empty")
        } else {
           print("> postURL: \(postURL ?? "")")
        }
        
        //timer
        var timer: Double = 10;
        let timerString:String? = command.arguments[2] as? String
        if timerString == nil || (timerString?.count ?? 0) == 0 {
            cordovaResultError(errorMessage: "timer input parameter cannot be empty")
        } else {
            timer = Double(timerString!) ?? 10
            print("> timer: \(timer)")
        }
        
        //request permissions if needed and start sending location
        let geoNotificationManager = GeoNotificationManager(trackedUUID: trackedUUID!, postURL: postURL!, timer: timer)
        if !geoNotificationManager.startLocation(){
            cordovaResultError(errorMessage: "Please enable location services to continue")
        } else {
            if !geoNotificationManager.registerPermissions() {
                cordovaResultError(errorMessage: "You must allow aloways access to location in order to use this application")
            } else {
                geoNotificationManager.startTimer()
            }
        }
        geoNotificationManager.registerPermissions()
    }
    
    func cordovaResultError(errorMessage: String){
        let result = CDVPluginResult(
            status: CDVCommandStatus_ERROR,
            messageAs: errorMessage
        )
        commandDelegate!.send(result, callbackId: command!.callbackId)
    }
    
    func cordovaResultSuccess(successMessage: String){
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: successMessage)
        
        commandDelegate!.send(result, callbackId: command!.callbackId)
    }
}

class GeoNotificationManager : NSObject, CLLocationManagerDelegate {
    let locationManager = CLLocationManager()
    
    var trackedUUID:String, postURL:String, timer:Double
    
    init(trackedUUID:String, postURL:String, timer:Double) {
        self.trackedUUID = trackedUUID
        self.postURL = postURL
        self.timer = timer;
        super.init()
    }
    
    func startLocation() -> Bool {
        if CLLocationManager.locationServicesEnabled() {
            locationManager.delegate = self
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
            locationManager.allowsBackgroundLocationUpdates = true
            locationManager.startUpdatingLocation()
            return true;
        } else {
            return false;
        }
    }
    
    func startTimer(){
        Timer.scheduledTimer(withTimeInterval: self.timer, repeats: true) { (Timer) in
             /*NSLog("timer!!! \(self.locationManager.location?.coordinate.latitude), \(self.locationManager.location?.coordinate.longitude)")*/
             
             if (self.locationManager.location?.coordinate != nil){
                 var returnJSONParameters: [String : Any] = [:];
                 //RSSI
                returnJSONParameters["uuid"] = self.trackedUUID;
                 
                returnJSONParameters["latitude"] = self.locationManager.location?.coordinate.latitude

                returnJSONParameters["longitude"] = self.locationManager.location?.coordinate.latitude

                let now = Date()
                let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "dd-MM-yyyy HH:mm:ss"
                returnJSONParameters["timeStamp"] = ("\(dateFormatter.string(from: now))")

                var error: Error?
                var jsonData: Data? = nil
                do {
                    jsonData = try JSONSerialization.data(withJSONObject: returnJSONParameters, options: .prettyPrinted)
                } catch {
                }
                if jsonData == nil && error != nil {
                    print("Error serializing JSON Object: \(error?.localizedDescription ?? "")")
                }

                let url = URL(string: self.postURL)
                var request = URLRequest(url: url!);

                request.httpMethod = "POST"
                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                request.httpBody = jsonData

                let defaultConfigObject = URLSessionConfiguration.default
                let session = URLSession(configuration: defaultConfigObject, delegate: nil, delegateQueue: OperationQueue.main)

                let task = session.dataTask(with: request, completionHandler: { data, response, error in
                 if error == nil {
                     var dicData: [AnyHashable : Any]? = nil
                     do {
                         if let data = data {
                             dicData = try JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [AnyHashable : Any]
                         }
                     } catch {
                     }
                     if let dicData = dicData {
                         print(">>> Response Data: \(dicData)")
                     }
                 } else {
                     print(">>> Error posting jsonData: \(error?.localizedDescription ?? "")")
                 }
                })
                task.resume()
                }
        }
    }
    
    func registerPermissions() -> Bool {
        if CLLocationManager.authorizationStatus() == .authorizedAlways {
            return true
        } else if ( CLLocationManager.authorizationStatus() == .notDetermined){
            locationManager.requestAlwaysAuthorization()
            return true
        } else {
            return false
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let locValue: CLLocationCoordinate2D = manager.location?.coordinate else { return }
        NSLog("locations = \(locValue.latitude) \(locValue.longitude)")
    }
}
