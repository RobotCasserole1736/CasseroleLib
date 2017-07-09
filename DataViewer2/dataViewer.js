//dataViewer.js
var global_chart;

//this set of presets is specific to 2017 - update as needed!
var presets = [["PDP_Voltage (V)", "PDP_Total_Current (A)", "Brownout_Protection_Active (bit)", "Battery_Dead (bit)"],
               ["PDP_Total_Current (A)", "PDP_DT_FL_Current (A)",  "PDP_DT_FR_Current (A)",  "PDP_DT_RL_Current (A)",  "PDP_DT_RR_Current (A)", "Hopper_Motor_Current (A)", "Climber_Motor1_Current (A)", "Climber_Motor2_Current (A)", "Intake_Motor_Current (A)", "Shooter_Motor_Current (A)"],
			   ["Shooter_Motor_Current (A)", "Shooter_Desired_Velocity (rpm)", "Shooter_Actual_Velocity (rpm)", "Shooter_Motor_Cmd (rpm)", "Shooter_Velocity_OK (bit)"],
               ["Robot_Pose_Angle (deg)", "Robot_Strafe_Vel (ft/sec)", "Robot_FwdRev_Vel (ft/sec)"],
               ["Shot_State_Command (bits)", "Shooter_Velocity_OK (bit)", "Shooter_Actual_Velocity (rpm)", "Hopper_Motor_Current (A)", "Shot_Counter (count)"],
			   ["Compressor_Current (A)", "Air_Pressure (psi)", "Gear_Solenoid_Cmd (bit)"],
			   ["RIO_Loop_Time (msec)", "RIO_Cpu_Load (%)", "RIO_RAM_Usage (%)"],
			   ["Vision_CoProc_FPS (frames/sec)", "Vision_CoProc_CPU_load (%)", "Vision_CoProc_Mem_load (%)"],
			   ["Vision_Align_Possible (bit)", "Valid_Vision_Target_Found (bit)", "Vision_System_Online (bit)", "Vision_System_Online (bit)", "Vision_Target_Angle_From_Camera (deg)", "Vision_Target_Range (ft)"],
			   ["Vision_DT_FwdRev_Cmd (cmd)", "Vision_DT_Rotate_Cmd (cmd)", "Vision_Align_On_Target (bit)", "Vision_Align_State (states)"],
			   ["Vision_Target_Gyro_Actual_Angle_At_Frame (deg)", "Vision_Target_Gyro_Desired_Angle_At_Frame (deg)", "Robot_Pose_Angle (deg)"],
               ["Vision_Target_Encoders_Actual_Distance_At_Frame (ft)", "Vision_Target_Encoders_Desired_Distance_At_Frame (ft)", "Robot_FwdRev_Dist (ft)"],
			   ["FL_Actual_Wheel_Velocity (rpm)", "FR_Actual_Wheel_Velocity (rpm)", "RL_Actual_Wheel_Velocity (rpm)", "RR_Actual_Wheel_Velocity (rpm)", "FL_Desired_Wheel_Velocity (rpm)", "FR_Desired_Wheel_Velocity (rpm)", "RL_Desired_Wheel_Velocity (rpm)", "RR_Desired_Wheel_Velocity (rpm)"],
			   ["Driver_FwdRev_cmd (cmd)", "Driver_Strafe_cmd (cmd)", "Driver_Rotate_cmd (cmd)", "Driver_Vision_Align_Desired (bit)", "Robot_Strafe_Vel (ft/sec)", "Robot_FwdRev_Vel (ft/sec)"]];

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
			text: ''
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
			enabled: true,
			itemMarginBottom: 2,
			itemMarginTop: 2,
			itemWidth: 300,
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
		
		colors: ['#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF', '#88FF00', '#8800FF', '#0088FF', '#FF8800',
		         '#FF0088', '#00FF88',
                ],
   
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
            },
        },

		series: []
	}


function handleFileSelect(files_in) {

    var fileobj = files_in[0];
    var temp_series = [];
    var units_to_yaxis_index = [];
    var yaxis_index = 0;


	//Destroy any existing chart.
	if(global_chart){
		global_chart.destroy();
	}
	
	//deep-copy the default chart options
	var options = $.extend(true, {}, dflt_options)


	var reader = new FileReader();
	reader.readAsText(fileobj);
    reader.onload = function(evt) {
    
		var all_lines = evt.target.result + '';
        var lines = all_lines.split('\n');
        var timestamp = 0;
        var plotter_index = 0;
		
		
        // Iterate over the lines and add categories or series
        $.each(lines, function(lineNo, line) {
            var items = line.split(',');
            
            // first line containes signal names. Ignore Time column.
            if (lineNo == 0) {
                plotter_index = 0;
                $.each(items, function(itemNo, item) {
                    if(itemNo > 0) {
                        if(item.length > 1){
                            temp_series.push({name:item.replace(/ /g,''),
                                              data:[],
                                              visible:false,
                                              visibility_counter:0,
                                              yAxis:0, //temp, will be updated once the actual unit is read.
                                              states: {
                                                  hover: {
                                                      enabled: false
                                                  },
                                              },
                                             });
                            plotter_index++;
                        }
                    }
                });
            }
            
            // second line containes units. Ignore Time column.
            else if (lineNo == 1) {
                plotter_index = 0;
                $.each(items, function(itemNo, item) {
                    if(itemNo > 0){
                        if(item.length > 1){
                            var unit = item.replace(/ /g,'');
                            temp_series[plotter_index].name = temp_series[plotter_index].name + ' (' + unit + ')';
                            if(!(unit in units_to_yaxis_index)){
                                units_to_yaxis_index[unit] = yaxis_index;
                                options.yAxis.push({
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
                            temp_series[plotter_index].yAxis = units_to_yaxis_index[unit];
                            plotter_index++;
                        }
                    } 
                });
            }
            
            // the rest of the lines contain data with their name in the first 
            // position
            else {
                plotter_index = 0;
                $.each(items, function(itemNo, item) {
                    if (itemNo == 0) {
                        timestamp = parseFloat(item);
                    } else {
                        if(item.length > 1){
                            temp_series[plotter_index].data.push([timestamp, parseFloat(item)]);
                            plotter_index++;
                        }
                    }
                });
                
            }
            
            
            
        });
        
		//Add all data to the chart
        $.each(temp_series, function(itemNo, element) {
            options.series.push(element);
        });
		
		//chart named after its sosurce file
		options.title.text = fileobj.name;
		
		//Create dat chart
		global_chart = new Highcharts.Chart(options);
		

		
	};
};

function setPreset(preset_idx){
	if(global_chart){	
		hideAll();
		for(itemNo = 0; itemNo < global_chart.series.length; itemNo++){
			for(preset_iter_idx = 0; preset_iter_idx < presets[preset_idx].length; preset_iter_idx++){
				if(global_chart.series[itemNo].name == presets[preset_idx][preset_iter_idx]){
					global_chart.series[itemNo].setVisible(true,false);
				}
			}
		}
		global_chart.redraw();
	}
}


function hideAll(){
	if(global_chart){	
		for(itemNo = 0; itemNo < global_chart.series.length; itemNo++){
			global_chart.series[itemNo].setVisible(false,false);
		}
	}
	global_chart.redraw();
}

