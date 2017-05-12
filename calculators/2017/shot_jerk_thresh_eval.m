fpath = ".\\log_21-Feb-2017_07.47.06PM_Teleop.csv";
data = csvread(fpath);
time = data(3:end,1);
shooterCurrent = data(3:end,16);

dt = diff(time);

%shooterCurrentFilt = zeros(length(shooterCurrent),1);
%shooterCurrentFilt(1) = shooterCurrent(1);

%for idx = 2:length(shooterCurrent)
%  shooterCurrentFilt(idx) = (shooterCurrent(idx) + shooterCurrent(idx-1))/2;
%end

Aps = (diff(shooterCurrent)./dt);


Apsps = diff(Aps)./dt(1:end-1);


ApspsFilt = zeros(length(Apsps),1);

ApspsFilt(1) = Apsps(1);
ApspsFilt(2) = Apsps(2);
ApspsFilt(3) = Apsps(3);
ApspsFilt(4) = Apsps(4);

for idx = 5:length(Apsps)
  ApspsFilt(idx) = (Apsps(idx) + Apsps(idx-1) + Apsps(idx-2) + Apsps(idx-3) + Apsps(idx-4))/5;
end

shotCount = zeros(length(ApspsFilt),1);
for idx = 2:length(shotCount)
  if((ApspsFilt(idx) < -4000) && (ApspsFilt(idx-1) >= -4000))
    shotCount(idx) = shotCount(idx-1)+1;
  else
    shotCount(idx) =shotCount(idx-1);
  end
end


%plot(time(1:end-2), shooterCurrent(1:end-2), time(1:end-2), ApspsFilt./100);
plot(time(1:end-2), shooterCurrent(1:end-2), time(1:end-2), shotCount);