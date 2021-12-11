package frc.team3128.hardware;

import edu.wpi.first.hal.SimDevice;
import edu.wpi.first.hal.SimDouble;
import edu.wpi.first.hal.SimDevice.Direction;
import edu.wpi.first.wpilibj.SpeedController;
import frc.team3128.common.Simulable;

import com.ctre.phoenix.motorcontrol.ControlMode;

import edu.wpi.first.wpilibj.SpeedController;

public abstract class NAR_Motor<T extends SpeedController> implements Simulable {

    // Condensed the contant-storing functionality of this class to an enum
    public static enum MotorType{

        Vex775Pro(18730, 0.7, 134, 0.71);

        private double freeSpeedRPM;
        private double freeCurrentAmps;
        private double stallCurrentAmps;
        private double stallTorqueNM;

        MotorType(double freeSpeedRPM, double freeCurrentAmps, double stallCurrentAmps, double stallTorqueNM){
            this.freeSpeedRPM = freeSpeedRPM;
            this.freeCurrentAmps = freeCurrentAmps;
            this.stallCurrentAmps = stallCurrentAmps;
            this.stallTorqueNM = stallTorqueNM;
        }

        public double getFreeSpeedRPM() {
            return freeSpeedRPM;
        }
    
        public double getFreeCurrentAmps() {
            return freeCurrentAmps;
        }
    
        public double getStallCurrentAmps () {
            return stallCurrentAmps;
        }
    
        public double getStallTorqueNM() {
            return stallTorqueNM;
        }
    }

    public static enum MotorControllerType {
        TALON_FX, TALON_SRX, VICTOR_SPX;
    }

    protected NAR_Motor(int deviceNumber) {
        this.deviceNumber = deviceNumber;
        construct();
    }

    public static NAR_Motor create(int deviceNumber, MotorControllerType motorType) {
        NAR_Motor result;
        switch(motorType) {
            case TALON_FX:
                result = new NAR_TalonFX(deviceNumber);
                break;
            case TALON_SRX:
                result =  new NAR_TalonSRX(deviceNumber);
                break;
            case VICTOR_SPX:
                result = new NAR_VictorSPX(deviceNumber);
                break;
            default:
                return null;
        }

        result.construct();
        return result;
    }

    // Have to be defined higher up
    protected double encoderRes;
    protected double moi;

    protected MotorType type;
    protected int deviceNumber;
    protected T motorController;
    protected SimDevice simEncoder;
    protected SimDouble simPos;
    protected SimDouble simVel;
    protected SimDouble simLoad;

    /**
     * Simulates physics for an individual motor assuming perfect conditions
     */
    @Override
    public void updateSimulation(double timeStep) {
        double position = simPos.get();
        double velocity = simVel.get();

        velocity+=getSimAcc()*timeStep;

        simVel.set(velocity);
        simPos.set(position+velocity*timeStep);
    }

    @Override
    public void constructFake(){
        simEncoder = SimDevice.create(motorController.getClass().getSimpleName()+"["+deviceNumber+"] simEncoder", deviceNumber);
        simPos = simEncoder.createDouble("Pos", Direction.kBidir, 0);
        simVel = simEncoder.createDouble("Vel", Direction.kBidir, 0);
        simLoad = simEncoder.createDouble("Load", Direction.kBidir, 0);
    }

    /**
     * @return Position in native units
     */
    public double getSimPos(){
        return simPos.get();
    }

    /**
     * @return Velocity in native units / second
     */
    public double getSimVel(){

        // freeSpeed in native units / second
        double freeSpeed = (type.getFreeSpeedRPM() * encoderRes / 60);

        if(simVel.get() < freeSpeed)
            return simVel.get();
        else
            return freeSpeed;
    }

    /**
     * @return Acceleration in native units / second squared
     */
    public double getSimAcc(){
        return getSimTorque() / moi;
    }

    /**
     * @return Net torque in N*m
     */
    public double getSimTorque(){

        double appTorq = type.getStallTorqueNM()*motorController.get();

        appTorq *= 1 - Math.abs(getSimVel()) / (type.getFreeSpeedRPM() * encoderRes / 60);

        return appTorq - simLoad.get();
    }

    /**
     * IMPLEMENT WITH CAUTION, this may require knowledge of more complete motor state
     * 
     * @param load simulated load in N*m
     */
    public void setSimLoad(double load){
        simLoad.set(load);
    }

    public void stop() {
        motorController.stopMotor();
    }

    public T getMotorController() {
        return (T) motorController;
    } 

    public SimDevice getFakeMotor(){
        return simEncoder;
    }

    public abstract void set(double value);
    public abstract void set(ControlMode controlMode, double value);
    public static enum NAR_NeutralMode{
        BRAKE, COAST;
    }
    public abstract void setNeutralMode(NAR_NeutralMode mode);
    public abstract double getSetpoint();
    public abstract double getEncoderPosition();
    public abstract double getEncoderVelocity();
    public abstract void setEncoderPosition(double pos);
}