%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% (C) 2016 FRC1736 Robot Casserole - All Rights Reserved
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% 2D Launched Ball Trajectory calculator
%% Includes air ressistance in model
%%
%%
%%  Changelog
%%  Jan 9, 2016 - Chris Gerth 
%%    - Created
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

clear;

%% Testcase constants - adjust to suit your needs (dependent on robot state at launch time)
launch_x_ft = 5;  %Distance between launch point and goal.  
launch_wheel_speed_RPM = 4050; %RPM speed of launch wheel (assumes single-wheel launcher)

%Your system parameters (dependent on robot design)
launch_z_ft = 1.0; %Launch point of the ball height off the ground in meters. Max is 1.37m
launch_angle_deg = 60; %angle between floor and launch point
launch_wheel_diameter_in = 4; %Launch wheel diameter in inches
launch_wheel_weight_lbs = 4; %Launch wheel mass in pounds


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Simulation constants - should probably stay as is.
Ts = 0.001; %1ms sample rate for solver
goal_min_height_m = 2.16; %lower edge of goal - 7ft 1 in
goal_max_height_m = 2.77; %Upper edge of goal - 9ft 1 in
ball_diameter_m = 0.254; % 10in ball 
ball_rad_m = ball_diameter_m/2;
g_mps = 9.81; %Gravitational constant
density_air_kgpm3 = 1.225; %desnity of air per https://en.wikipedia.org/wiki/Density_of_air
m_ball_kg = 0.1; %Total guess at the weight of the "boulders" in kg.
cD = 0.47; %Drag coefficent for sphere, per https://en.wikipedia.org/wiki/Drag_coefficient
%cD = 0; %Zero drag = no air resistance = you can't breathe


%% Derived constants
frontal_area_m2 = 0.5 * (4*pi*(0.5*ball_diameter_m)^2); %Half the total surface area is the frontal area
Vt_mps = sqrt((2*m_ball_kg*g_mps)/(cD*density_air_kgpm3*frontal_area_m2)); %Terminal velocity in meters per second
launch_angle_rad = pi/180*launch_angle_deg;
launch_x_m = 0.3048*launch_x_ft;
launch_z_m = 0.3048*launch_z_ft;
launch_wheel_weight_kg = launch_wheel_weight_lbs * 0.4535;
launch_wheel_rad_m = 0.5*0.0254*launch_wheel_diameter_in;
launch_wheel_tan_v_mps = launch_wheel_rad_m*launch_wheel_speed_RPM*0.1047;

i = 1; %simulation step

%Initial velocity calculations
% Based off of http://lynbrookrobotics.com/resourcefiles/whitepages/2012/Shooter%20Calculations%20Document.pdf
launch_speed_mps = 0.5*launch_wheel_tan_v_mps/((launch_wheel_weight_kg+7/5*m_ball_kg)/launch_wheel_weight_kg);


%See http://farside.ph.utexas.edu/teaching/336k/Newtonhtml/node29.html for some 
%references on trajectory calculation

%Initial conditions
time(i) = i*Ts;
pos_x(i) = 0;
pos_z(i) = launch_z_m;
vel_x(i) = launch_speed_mps*cos(launch_angle_rad);
vel_z(i) = launch_speed_mps*sin(launch_angle_rad);



%calculate trajectory until terminal case (ball hits floor or ball hits castle wall)
while( and(pos_z(i) > 0.5*ball_diameter_m,  pos_x(i) < launch_x_m))
  i = i + 1;
  time(i) = i*Ts;
  vel_x(i) = vel_x(i-1)/(1+Ts*g_mps/Vt_mps);
  vel_z(i) = (-Ts*g_mps + vel_z(i-1))/(1+Ts*g_mps/Vt_mps);
  pos_x(i) = pos_x(i-1) + vel_x(i)*Ts;
  pos_z(i) = pos_z(i-1) + vel_z(i)*Ts;
endwhile


figure(1);
clf;
hold on;

%draw floor and goal
rectangle('Position', [0,-0.05,launch_x_m,0.05]./0.3048, 'FaceColor', [0,0,0]);
rectangle('Position', [launch_x_m,0,0.05,goal_min_height_m]./0.3048, 'FaceColor', [1,0,0]);
rectangle('Position', [launch_x_m,goal_max_height_m,0.05,0.3]./0.3048, 'FaceColor', [1,0,0]);
%draw ball
rectangle('Position', [pos_x(i)-ball_rad_m,pos_z(i)-ball_rad_m,2*ball_rad_m,2*ball_rad_m]./0.3048, 'FaceColor', [0.5, 0.5, 0.5], 'Curvature', [1, 1]);


%Plot trajectory
plot(pos_x./0.3048, pos_z./0.3048);
axis equal;


