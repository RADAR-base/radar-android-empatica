/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package org.radarcns.pebble;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
/** Raw heart rate from the Pebble 2 Heart Monitor. You can compute the inter beat interval rate as (60 / heartRate). */
@org.apache.avro.specific.AvroGenerated
public class Pebble2HeartRate extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -5504532213160600054L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Pebble2HeartRate\",\"namespace\":\"org.radarcns.pebble\",\"doc\":\"Raw heart rate from the Pebble 2 Heart Monitor. You can compute the inter beat interval rate as (60 / heartRate).\",\"fields\":[{\"name\":\"time\",\"type\":\"double\",\"doc\":\"device timestamp in UTC (s)\"},{\"name\":\"timeReceived\",\"type\":\"double\",\"doc\":\"device receiver timestamp in UTC (s)\"},{\"name\":\"heartRate\",\"type\":\"float\",\"doc\":\"heart rate (bpm)\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  /** device timestamp in UTC (s) */
  @Deprecated public double time;
  /** device receiver timestamp in UTC (s) */
  @Deprecated public double timeReceived;
  /** heart rate (bpm) */
  @Deprecated public float heartRate;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Pebble2HeartRate() {}

  /**
   * All-args constructor.
   * @param time device timestamp in UTC (s)
   * @param timeReceived device receiver timestamp in UTC (s)
   * @param heartRate heart rate (bpm)
   */
  public Pebble2HeartRate(java.lang.Double time, java.lang.Double timeReceived, java.lang.Float heartRate) {
    this.time = time;
    this.timeReceived = timeReceived;
    this.heartRate = heartRate;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return time;
    case 1: return timeReceived;
    case 2: return heartRate;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: time = (java.lang.Double)value$; break;
    case 1: timeReceived = (java.lang.Double)value$; break;
    case 2: heartRate = (java.lang.Float)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'time' field.
   * @return device timestamp in UTC (s)
   */
  public java.lang.Double getTime() {
    return time;
  }

  /**
   * Sets the value of the 'time' field.
   * device timestamp in UTC (s)
   * @param value the value to set.
   */
  public void setTime(java.lang.Double value) {
    this.time = value;
  }

  /**
   * Gets the value of the 'timeReceived' field.
   * @return device receiver timestamp in UTC (s)
   */
  public java.lang.Double getTimeReceived() {
    return timeReceived;
  }

  /**
   * Sets the value of the 'timeReceived' field.
   * device receiver timestamp in UTC (s)
   * @param value the value to set.
   */
  public void setTimeReceived(java.lang.Double value) {
    this.timeReceived = value;
  }

  /**
   * Gets the value of the 'heartRate' field.
   * @return heart rate (bpm)
   */
  public java.lang.Float getHeartRate() {
    return heartRate;
  }

  /**
   * Sets the value of the 'heartRate' field.
   * heart rate (bpm)
   * @param value the value to set.
   */
  public void setHeartRate(java.lang.Float value) {
    this.heartRate = value;
  }

  /**
   * Creates a new Pebble2HeartRate RecordBuilder.
   * @return A new Pebble2HeartRate RecordBuilder
   */
  public static org.radarcns.pebble.Pebble2HeartRate.Builder newBuilder() {
    return new org.radarcns.pebble.Pebble2HeartRate.Builder();
  }

  /**
   * Creates a new Pebble2HeartRate RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Pebble2HeartRate RecordBuilder
   */
  public static org.radarcns.pebble.Pebble2HeartRate.Builder newBuilder(org.radarcns.pebble.Pebble2HeartRate.Builder other) {
    return new org.radarcns.pebble.Pebble2HeartRate.Builder(other);
  }

  /**
   * Creates a new Pebble2HeartRate RecordBuilder by copying an existing Pebble2HeartRate instance.
   * @param other The existing instance to copy.
   * @return A new Pebble2HeartRate RecordBuilder
   */
  public static org.radarcns.pebble.Pebble2HeartRate.Builder newBuilder(org.radarcns.pebble.Pebble2HeartRate other) {
    return new org.radarcns.pebble.Pebble2HeartRate.Builder(other);
  }

  /**
   * RecordBuilder for Pebble2HeartRate instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Pebble2HeartRate>
    implements org.apache.avro.data.RecordBuilder<Pebble2HeartRate> {

    /** device timestamp in UTC (s) */
    private double time;
    /** device receiver timestamp in UTC (s) */
    private double timeReceived;
    /** heart rate (bpm) */
    private float heartRate;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(org.radarcns.pebble.Pebble2HeartRate.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.time)) {
        this.time = data().deepCopy(fields()[0].schema(), other.time);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.timeReceived)) {
        this.timeReceived = data().deepCopy(fields()[1].schema(), other.timeReceived);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.heartRate)) {
        this.heartRate = data().deepCopy(fields()[2].schema(), other.heartRate);
        fieldSetFlags()[2] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing Pebble2HeartRate instance
     * @param other The existing instance to copy.
     */
    private Builder(org.radarcns.pebble.Pebble2HeartRate other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.time)) {
        this.time = data().deepCopy(fields()[0].schema(), other.time);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.timeReceived)) {
        this.timeReceived = data().deepCopy(fields()[1].schema(), other.timeReceived);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.heartRate)) {
        this.heartRate = data().deepCopy(fields()[2].schema(), other.heartRate);
        fieldSetFlags()[2] = true;
      }
    }

    /**
      * Gets the value of the 'time' field.
      * device timestamp in UTC (s)
      * @return The value.
      */
    public java.lang.Double getTime() {
      return time;
    }

    /**
      * Sets the value of the 'time' field.
      * device timestamp in UTC (s)
      * @param value The value of 'time'.
      * @return This builder.
      */
    public org.radarcns.pebble.Pebble2HeartRate.Builder setTime(double value) {
      validate(fields()[0], value);
      this.time = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'time' field has been set.
      * device timestamp in UTC (s)
      * @return True if the 'time' field has been set, false otherwise.
      */
    public boolean hasTime() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'time' field.
      * device timestamp in UTC (s)
      * @return This builder.
      */
    public org.radarcns.pebble.Pebble2HeartRate.Builder clearTime() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'timeReceived' field.
      * device receiver timestamp in UTC (s)
      * @return The value.
      */
    public java.lang.Double getTimeReceived() {
      return timeReceived;
    }

    /**
      * Sets the value of the 'timeReceived' field.
      * device receiver timestamp in UTC (s)
      * @param value The value of 'timeReceived'.
      * @return This builder.
      */
    public org.radarcns.pebble.Pebble2HeartRate.Builder setTimeReceived(double value) {
      validate(fields()[1], value);
      this.timeReceived = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'timeReceived' field has been set.
      * device receiver timestamp in UTC (s)
      * @return True if the 'timeReceived' field has been set, false otherwise.
      */
    public boolean hasTimeReceived() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'timeReceived' field.
      * device receiver timestamp in UTC (s)
      * @return This builder.
      */
    public org.radarcns.pebble.Pebble2HeartRate.Builder clearTimeReceived() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'heartRate' field.
      * heart rate (bpm)
      * @return The value.
      */
    public java.lang.Float getHeartRate() {
      return heartRate;
    }

    /**
      * Sets the value of the 'heartRate' field.
      * heart rate (bpm)
      * @param value The value of 'heartRate'.
      * @return This builder.
      */
    public org.radarcns.pebble.Pebble2HeartRate.Builder setHeartRate(float value) {
      validate(fields()[2], value);
      this.heartRate = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'heartRate' field has been set.
      * heart rate (bpm)
      * @return True if the 'heartRate' field has been set, false otherwise.
      */
    public boolean hasHeartRate() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'heartRate' field.
      * heart rate (bpm)
      * @return This builder.
      */
    public org.radarcns.pebble.Pebble2HeartRate.Builder clearHeartRate() {
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    public Pebble2HeartRate build() {
      try {
        Pebble2HeartRate record = new Pebble2HeartRate();
        record.time = fieldSetFlags()[0] ? this.time : (java.lang.Double) defaultValue(fields()[0]);
        record.timeReceived = fieldSetFlags()[1] ? this.timeReceived : (java.lang.Double) defaultValue(fields()[1]);
        record.heartRate = fieldSetFlags()[2] ? this.heartRate : (java.lang.Float) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}
