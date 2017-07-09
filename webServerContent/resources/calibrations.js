
//Note - this PORT string must be aligned with the port the webserver is served on.
var port = "5805";
var hostname = window.location.hostname+":"+port;

var dataSocket = new WebSocket("ws://"+hostname+"/calstream")

dataSocket.onopen = function (event) {
  document.getElementById("id01").innerHTML = "COM Status: Socket Opened.";
};

dataSocket.onerror = function (error) {
  document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
  alert("ERROR from Calibration: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
};

dataSocket.onclose = function (error) {
  document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
  alert("ERROR from Calibration: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
};

dataSocket.onmessage = function (event) {
  var data = JSON.parse(event.data);
  if(data.type == "msg"){
    alert(data.msg_text);
  } else if(data.type == "cal_vals"){
    genTable(data);
  }

};


function genTable(arr) {
    
    var i;
    var out = "<table border=\"1\">";
    
    out += "<tr><td>" +
           "Cal Name" +
           "</td><td style=\"width: 100px;\">" +
           "Default Val" +
           "</td><td style=\"width: 100px;\">" +
           "Minimum Val" +
           "</td><td style=\"width: 100px;\">" +
           "Maximum Val" +
           "</td><td style=\"width: 75px;\">" +
           "Overridden" +
           "</td><td style=\"width: 100px;\">" +
           "Current Val" +
           "</td><td style=\"width: 100px;\">" +
           "New Val" +
           "</td><td>" + //blank col. for apply button
           "</td><td>" + //blank col. for reset button
           "</td></tr>";
    
    for(i = 0; i < arr.cal_array.length; i++) {
        out += "<tr><td>" +
               arr.cal_array[i].name +
               "</td><td style=\"width: 100px;\">" +
               arr.cal_array[i].dflt_val +
               "</td><td style=\"width: 100px;\">" +
               arr.cal_array[i].min_val +
               "</td><td style=\"width: 100px;\">" +
               arr.cal_array[i].max_val +
               "</td><td style=\"width: 75px;\">" +
               arr.cal_array[i].ovrdn +
               "</td><td style=\"width: 100px;\">" +
               arr.cal_array[i].cur_val +
               "</td><td style=\"width: 100px;\">" +
               "<input type=\"number\" name=\""+ arr.cal_array[i].name +"\" id=\""+arr.cal_array[i].name+"_submit_val\">" +
               "</td><td>" + 
               "<button id=\""+arr.cal_array[i].name+"_submit_btn\" type=\"button\" onclick=\"handleApplyButtonClick("+arr.cal_array[i].name+"_submit_val"+")\">Apply</button>" +
               "</td><td>" + 
               "<button id=\""+arr.cal_array[i].name+"_reset_btn\" type=\"button\" onclick=\"handleResetButtonClick("+arr.cal_array[i].name+"_submit_val"+")\">Reset</button>" +
               "</td></tr>";
    }
    out += "</table>";
    document.getElementById("id02").innerHTML = out;
}

function handleApplyButtonClick(input_box){
    var val_string = input_box.value;
    var val = parseFloat(val_string);
    
    if(val == NaN){
        document.getElementById("id01").innerHTML = "Error: " + val_string + " is not a number!";
    } else {
        dataSocket.send("set:"+input_box.name+":"+val); 
    }

}

function handleResetButtonClick(input_box){
    dataSocket.send("reset:"+input_box.name); 
}

function handleSaveBtnClick(){
    dataSocket.send("save"); 
}
