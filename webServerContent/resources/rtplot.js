
///////////////////////////////////////////////////////////////////////////////////
// Main code - executed on page load.
///////////////////////////////////////////////////////////////////////////////////
//Note - this PORT string must be aligned with the port the webserver is served on.
// We'll hardcode for now, but this must remain aligned with the server java implementation
var port = "5805";
var hostname = window.location.hostname+":"+port;

//Set up the web socket
var dataSocket = new WebSocket("ws://"+hostname+"/rtplot")

var signal_names = []
var signal_units = []
var signal_display_names = []

var allow_scroll_zoom = true;

var mouse_cursor_center = null;

var global_chart;

var local_storage_available = false;

var time_range_sec = 10.0;

var sq = {};

var ls_sel_signals= []; //String array of the start-checked boxes from local storage

//Set up Local Storage
// LocalStorage is a javascript feature which allows you to store some string
// on this particular client's PC. The usecase we'll be doing here is to remember
// what checkboxes the user had checked so the next time they open the tab, we'll 
// hit those same checkboxes. There's a couple things to consider here:
// -- Robot code changed, so a checkbox which previously existed won't exist anymore
// -- Local storage may not be available on the client.
if (typeof(Storage) !== "undefined") {
    local_storage_available = true;
} else {
    local_storage_available = false;
}

//The following chunk of main code and handler functions are to add chart interaction
// which I find useful, but which highcharts does not implement natively.
// Namely, I want a mouse-wheel zoom, which centeres around wherever the user's mouse
// is sitting at.

sq.e = document.getElementById("container");

if (sq.e.addEventListener) {
    sq.e.addEventListener("mousewheel", ChartMouseWheelHandler, false);
    sq.e.addEventListener("DOMMouseScroll", ChartMouseWheelHandler, false);
}
else sq.e.attachEvent("onmousewheel", ChartMouseWheelHandler);

sq.e.addEventListener('mousemove', ChartMouseMoveHandler, false);

///////////////////////////////////////////////////////////////////////////////////
// End Main Code
///////////////////////////////////////////////////////////////////////////////////


//sets mouse_cursor_center to an X value if possible, otherwise null
// This is used by the zoom function to determine where the zoom center should be.
function ChartMouseMoveHandler(e){
    if(global_chart){
        e = global_chart.pointer.normalize(e);
        if(global_chart.isInsidePlot(e.chartX - global_chart.plotLeft, e.chartY - global_chart.plotTop)){
            mouse_cursor_center = global_chart.xAxis[0].toValue(e.chartX);
        } else {
            mouse_cursor_center = null;
        }
        
    }
    
}

//Handle mouse wheel ticks to change the zoom on the chart 
function ChartMouseWheelHandler(e) {
    // cross-browser wheel delta
    var e = window.event || e;
    var delta = Math.max(-1, Math.min(1, (e.wheelDelta || -e.detail)));
    
    if(global_chart){
        if(allow_scroll_zoom){
            
            //We should adjust the view extents based on the read-in mouse wheel scroll action
            
            //Grab the current view level (min/max data range)
            old_max = global_chart.xAxis[0].getExtremes().max;
            old_min = global_chart.xAxis[0].getExtremes().min;
            
            //Calculate the zoom action center (presumed to be center of chart if not yet set)
            if(mouse_cursor_center != null){
                center = mouse_cursor_center;
            } else {
                center = (old_max + old_min)/2;
            }
            
            
            //Calcualte the above/below center widths
            old_right_size = old_max - center;
            old_left_size = center - old_min;
            
            //Calcualte a multiclipative factor (1.0 for no change, > 1.0 for more zoom, < 1.0 for less zoom)
            scaler = (1-delta*0.1);
            
            //Apply the factor to the above/below center widths
            new_right_size = old_right_size * scaler;
            new_left_size = old_left_size * scaler;
            
            //calcualte the new extents
            new_max = center + new_right_size;
            new_min = center - new_left_size;
            
            //Limit the extents to the available data. Make sure to display the
            // "reset zoom" button if we're not yet looking at the whole chart.
            if(global_chart.xAxis[0].getExtremes().dataMin > new_min){
                new_min = global_chart.xAxis[0].getExtremes().dataMin;
            } 
            
            if(global_chart.xAxis[0].getExtremes().dataMax < new_max){
                new_max = global_chart.xAxis[0].getExtremes().dataMax;
            }
            
            //Set the new extents
            global_chart.xAxis[0].setExtremes(new_min, new_max);
            
        }
    }
    
    //Return false to prevent this mouse event we're handling here from scrolling the page
    if (e.preventDefault)e.preventDefault();
    return false;
}





// The following handlers are for events generated from the datasocket opened to the robot
// which streams data back and forth.

dataSocket.onopen = function (event) {
    document.getElementById("id01").innerHTML = "COM Status: Socket Opened.";
    document.getElementById("stop_btn").disabled = true;
    document.getElementById("start_btn").disabled = false;
};

dataSocket.onerror = function (error) {
    document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
    alert("ERROR from RT Plot: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
    document.getElementById("stop_btn").disabled = true;
    document.getElementById("start_btn").disabled = true;
};

dataSocket.onclose = function (error) {
    document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
    alert("ERROR from RT Plot: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
    document.getElementById("stop_btn").disabled = true;
    document.getElementById("start_btn").disabled = true;
};

dataSocket.onmessage = function (event) {
    var data = JSON.parse(event.data);
    if(data.type == "daq_update"){
        addDataToPlot(data.samples);
    } else if(data.type == "signal_list"){
        genSignalListTable(data.signals);
    }

};

// The following are utility functions to handle data received over the dataSocket or from user inputs.

//Given a chunk of json data which is presumed to represent a set of 
// samples for the currently-plotted signals, add that data to the plot.
function addDataToPlot(data){
    var sig_iter;
    var samp_iter;
    var samp_time;
    var samp_val;
    var newest_timestamp = 0;
    
    //Iterate over all samples in all signals recieved
    for(sig_iter = 0; sig_iter < data.length; sig_iter++){
        for(samp_iter = 0; samp_iter < data[sig_iter].samples.length; samp_iter++){
            
            //Parse each sample time&value
            samp_time = parseFloat(data[sig_iter].samples[samp_iter].time);
            samp_val = parseFloat(data[sig_iter].samples[samp_iter].val);
            
            //Keep track of the most recent sample of all the data
            if(samp_time > newest_timestamp){
                newest_timestamp = samp_time;
            }
            
            //Add the saple to the plot
            global_chart.series[sig_iter].addPoint([samp_time,samp_val],false,false,true);
        }
    }
    
    global_chart.xAxis[0].setExtremes(newest_timestamp - time_range_sec,newest_timestamp,false)
    //Force a chart update to display the table
    global_chart.redraw();
    
}

function genSignalListTable(arr){
    var i;
    var col_counter = 0;
    var SIGNALS_PER_ROW = 1; //meh. html is hard.
    signal_names = [];
    var checked_state = ""; //String to be injected into the html of the checkbox declaration if the checkbox should start checked
    
    //Init some content for the dynamic HTML which will show checkboxes for each signal
    var out = "<table><tbody><tr>";
    
    //Read the desired signals from selected signals
    if(local_storage_available == true){
        ls_sel_signals = JSON.parse(localStorage.getItem("CasseroleRTPlot"))
        if(ls_sel_signals == null){
            ls_sel_signals = [];
        }
    }
    
    for(i = 0; i < arr.length; i++){
        //Record the signal info in local arrays for later use (when starting a new recording)
        signal_names.push(arr[i].name);
        signal_units.push(arr[i].units);
        signal_display_names.push(arr[i].display_name);
        
        //See if this signal's name is in the local storage list
        if(ls_sel_signals.indexOf(arr[i].name) > -1){
            //If it is, we'll inject some HTML magic to make the box start out checked
            checked_state = "checked=\"checked\"";
        } else {
            checked_state = "";
        }
        
        //Add some html to display a checkbox for this signal
        out += "<td><input type=\"checkbox\" name=\""+arr[i].name+"\" " + checked_state + " />"+arr[i].display_name+" (" + arr[i].units + ") </td></tr><tr>";
        
    }
    
    //Close out the HTML and push it to the document for display.
    out +="</tr></tbody></table>";
    document.getElementById("id02").innerHTML = out;

}

function handleStartBtnClick(){
    var cmd = "start:";
    var temp_series = [];
    var units_to_yaxis_index = [];
    var yaxis_index = 0;
    
    
    if(local_storage_available){
        //Clear local storage if available
        ls_sel_signals = [];
    }
    
    //deep-copy the default chart options
	var options = $.extend(true, {}, dflt_options)
    
    allow_scroll_zoom = false;
    
    //Destroy any existing chart.
	if(global_chart){
        //double check the user didn't click it by mistake.
        if(confirm('This will clear the current recording. Are you sure?')){
            global_chart.destroy();
        } else {
            return; //do nothing
        }
		
	}
	
    
    //Disable signal selection
    document.getElementById("clear_btn").disabled = true;
    document.getElementById("start_btn").disabled = true;
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            checkboxes[j].disabled = true;
        }
    }
    
    //Select only checked signals
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            
            //For all checked boxes...
            if(checkboxes[j].checked == true){
                
                if(local_storage_available){
                    //Record that the box was checked if local storage is available
                    ls_sel_signals.push(signal_names[i]);
                }
                
                //Assemble command for sending to server
                cmd += signal_names[i] + ",";
                
                //Handle grouping like-units signals on the same Y axis
                var unit = signal_units[i];
                if(!(unit in units_to_yaxis_index)){
                    units_to_yaxis_index[unit] = yaxis_index;
                    options.yAxis.push({    //All this is config for highcharts to make stuff look pretty
                                            title:{
                                                text:unit,
                                                style: {
                                                    color: '#DDD',
                                                },
                                            }, 
                                            showEmpty:false,
                                            lineColor: '#777',
                                            tickColor: '#444',
                                            gridLineColor: '#444',
                                            gridLineWidth: 1,
                                            labels: {
                                                style: {
                                                    color: '#DDD',
                                                    fontWeight: 'bold'
                                                },
                                            },
                                       });
                    yaxis_index++;
                }
                
                // set up chart for signals
                temp_series.push({name:signal_display_names[i],
                                  data:[],
                                  visible:true,
                                  visibility_counter:0,
                                  yAxis:units_to_yaxis_index[unit],
                                  states: {
                                      hover: {
                                          enabled: false
                                      },
                                  },
                                  marker: {
                                      enabled: null
                                  },
                                 });

            }
            

    
            
        }
    }
    
    //Update local storage
    if(local_storage_available){
        localStorage.setItem("CasseroleRTPlot", JSON.stringify(ls_sel_signals));
    }
    
    
    //Create the Highcharts chart just before starting DAQ
        
    //Add all data to the chart
    $.each(temp_series, function(itemNo, element) {
        options.series.push(element);
    });
    
    //Create dat chart
    global_chart = new Highcharts.Chart(options);

    //Request data from robot
    dataSocket.send(cmd); 
    document.getElementById("stop_btn").disabled = false;
}

function handleStopBtnClick(){
    //Request stopping data from robot
    dataSocket.send("stop:"); 
    
    document.getElementById("stop_btn").disabled = true;
    
    //re-enable siagnal selection
    document.getElementById("clear_btn").disabled = false;
    document.getElementById("start_btn").disabled = false;
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            checkboxes[j].disabled = false;
        }
    }
    
    allow_scroll_zoom = true;
    
    //Reset chart bounds to all data recieved.
    global_chart.xAxis[0].setExtremes(null,null)
}

function handleRefreshSignalsBtnClick(){
    dataSocket.send("get_list:"); 
}

function handleClearBtnClick(){
    var i;
    //Reset all checkboxes to unchecked.
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            checkboxes[j].checked = false;
        }
    }
    
    //Reset localStorage
    if(local_storage_available){
        ls_sel_signals = [];
        localStorage.setItem("CasseroleRTPlot", JSON.stringify(ls_sel_signals));
    }

}

/**************************************************************************************
 ** HIGHCHARTS SUPPORT
 **************************************************************************************/

var dflt_options =  {    

		credits: {
			enabled: false
		},

		chart: {
			zoomType: 'x',
			renderTo: 'container',
			animation: false,
			ignoreHiddenSeries: true,
            resetZoomButton: {
                position: {
                    align: 'left',
                },
                theme: {
                    fill: '#822',
                    stroke: '#999',
                    r: 3,
                    style: {
                        color: '#999'
                    },
                    states: {
                        hover: {
                            fill: '#782828',
                            style: {
                                color: '#ccc'
                            },
                        },
                    },
                },
            },
			panning: true,
			panKey: 'shift',
			showAxes: true,
            backgroundColor: {
                linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                stops: [
                    [0, 'rgb(0, 0, 0)'], //Yes, both black. Just in case I decide to change back....
                    [1, 'rgb(0, 0, 0)']
                ]
            },
		},
        
		title: { 
            //disable title
			text: null,
		},
        
		xAxis: {
			type: 'linear',
			title: 'Time (sec)',
            lineColor: '#777',
            tickColor: '#444',
            gridLineColor: '#444',
            gridLineWidth: 1,
            labels: {
                style: {
                    color: '#DDD',
                    fontWeight: 'bold'
                },
            },
            title: {
                style: {
                    color: '#D43',
                },
            },
		},
		
    	yAxis: [],
		
		legend: {
			layout: 'vertical',
            align: 'right',
            verticalAlign: 'top',
            borderWidth: 1,
            floating: true,
            itemStyle: {
                font: '9pt Trebuchet MS, Verdana, sans-serif',
                color: '#DDD'
            },
            itemHoverStyle:{
                color: 'gray'
            }  
			
		},
		
		exporting: {
			enabled: false
		},
		
		colors: ['#FF0000', '#0000FF', '#00FF00','#FF00FF', '#00FFFF', '#FFFF00'],
   
		plotOptions: {
			line: {
				marker: {
					radius: 2
				},
				lineWidth: 1,
				threshold: null,
				animation: false,
			}
		},
		tooltip: {
			crosshairs: true,
			hideDelay: 0,
			shared: true,
			backgroundColor: null,
            snap: 30,
			borderWidth: 1,
            borderColor: '#FF0000',
			shadow: true,
			animation: false,
			useHTML: false,
			style: {
                padding: 0,
                color: '#D43',
            }
        },  

		series: []
	}