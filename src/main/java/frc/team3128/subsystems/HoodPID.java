package frc.team3128.subsystems;

import frc.team3128.Constants.HoodConstants;
import frc.team3128.Constants.ShooterConstants;

import com.revrobotics.SparkMaxRelativeEncoder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotController;
import frc.team3128.common.hardware.motorcontroller.NAR_CANSparkMax;
import frc.team3128.common.infrastructure.NAR_PIDSubsystem;
import net.thefletcher.revrobotics.enums.IdleMode;
import net.thefletcher.revrobotics.enums.MotorType;

public class HoodPID extends NAR_PIDSubsystem {

    private static Hood instance;
    private NAR_CANSparkMax m_hoodMotor;
    private SparkMaxRelativeEncoder m_encoder;
    
    private double tolerance = HoodConstants.TOLERANCE_MIN;

    private double time;
    private double prevTime;

    public static synchronized Hood getInstance() {
        if(instance == null) {
            instance = new Hood();
        }
        return instance;
    }

    public HoodPID() {
        super(new PIDController(HoodConstants.kP, HoodConstants.kI, HoodConstants.kD), HoodConstants.PLATEAU_COUNT);

        configMotors();
        configEncoder();
    }

    private void configMotors() {
        m_hoodMotor = new NAR_CANSparkMax(HoodConstants.HOOD_MOTOR_ID, MotorType.kBrushless);
        m_hoodMotor.setSmartCurrentLimit(HoodConstants.HOOD_CURRENT_LIMIT);
        m_hoodMotor.enableVoltageCompensation(12.0);
        m_hoodMotor.setIdleMode(IdleMode.kBrake);
    }

    private void configEncoder() {
        m_encoder = (SparkMaxRelativeEncoder) m_hoodMotor.getEncoder();
        m_encoder.setPositionConversionFactor(HoodConstants.ENC_POSITION_CONVERSION_FACTOR);
        zeroEncoder();
    }

    public void setSpeed(double speed) {
        m_hoodMotor.set(speed);
    }

    public void stop() {
        m_hoodMotor.set(0);
    }

    /**
     * Encoder returns 0 deg when at min angle.
     */
    public void zeroEncoder() {
        m_hoodMotor.setEncoderPosition(0);
    }

    public void startPID(double angle) {
        tolerance = ShooterConstants.RPM_THRESHOLD_PERCENT;
        super.setSetpoint(angle);  
        super.resetPlateauCount();
        getController().setTolerance(tolerance);
    }

    /**
     * Attempts to PID to minimum angle. Will likely be replaced by full homing routine once limit switch is added.
     */
    public void zero() {
        startPID(HoodConstants.MIN_ANGLE);
    }

    @Override
    protected void useOutput(double output, double setpoint) {
        double ff = HoodConstants.kF * Math.cos(Units.degreesToRadians(setpoint));
        double voltageOutput = output + ff;

        time = RobotController.getFPGATime() / 1e6;
        if (tolerance < HoodConstants.TOLERANCE_MAX) {
            tolerance += (time - prevTime) * (HoodConstants.TOLERANCE_MAX - HoodConstants.TOLERANCE_MIN) / HoodConstants.TIME_TO_MAX_TOLERANCE;
            getController().setTolerance(tolerance);
        }

        checkPlateau(setpoint, tolerance);

        m_hoodMotor.set(voltageOutput / 12.0);

        prevTime = time;
    }

    @Override
    protected double getMeasurement() {
        return m_hoodMotor.getSelectedSensorPosition() + HoodConstants.MIN_ANGLE;
    }

}
