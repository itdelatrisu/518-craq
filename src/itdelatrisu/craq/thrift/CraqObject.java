/**
 * Autogenerated by Thrift Compiler (0.10.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package itdelatrisu.craq.thrift;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
/**
 * Object envelope.
 */
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.10.0)", date = "2017-05-09")
public class CraqObject implements org.apache.thrift.TBase<CraqObject, CraqObject._Fields>, java.io.Serializable, Cloneable, Comparable<CraqObject> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("CraqObject");

  private static final org.apache.thrift.protocol.TField VALUE_FIELD_DESC = new org.apache.thrift.protocol.TField("value", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField DIRTY_FIELD_DESC = new org.apache.thrift.protocol.TField("dirty", org.apache.thrift.protocol.TType.BOOL, (short)2);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new CraqObjectStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new CraqObjectTupleSchemeFactory();

  public java.nio.ByteBuffer value; // optional
  public boolean dirty; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    VALUE((short)1, "value"),
    DIRTY((short)2, "dirty");

    private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // VALUE
          return VALUE;
        case 2: // DIRTY
          return DIRTY;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(java.lang.String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final java.lang.String _fieldName;

    _Fields(short thriftId, java.lang.String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public java.lang.String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __DIRTY_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  private static final _Fields optionals[] = {_Fields.VALUE,_Fields.DIRTY};
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.VALUE, new org.apache.thrift.meta_data.FieldMetaData("value", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING        , true)));
    tmpMap.put(_Fields.DIRTY, new org.apache.thrift.meta_data.FieldMetaData("dirty", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(CraqObject.class, metaDataMap);
  }

  public CraqObject() {
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public CraqObject(CraqObject other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetValue()) {
      this.value = org.apache.thrift.TBaseHelper.copyBinary(other.value);
    }
    this.dirty = other.dirty;
  }

  public CraqObject deepCopy() {
    return new CraqObject(this);
  }

  @Override
  public void clear() {
    this.value = null;
    setDirtyIsSet(false);
    this.dirty = false;
  }

  public byte[] getValue() {
    setValue(org.apache.thrift.TBaseHelper.rightSize(value));
    return value == null ? null : value.array();
  }

  public java.nio.ByteBuffer bufferForValue() {
    return org.apache.thrift.TBaseHelper.copyBinary(value);
  }

  public CraqObject setValue(byte[] value) {
    this.value = value == null ? (java.nio.ByteBuffer)null : java.nio.ByteBuffer.wrap(value.clone());
    return this;
  }

  public CraqObject setValue(java.nio.ByteBuffer value) {
    this.value = org.apache.thrift.TBaseHelper.copyBinary(value);
    return this;
  }

  public void unsetValue() {
    this.value = null;
  }

  /** Returns true if field value is set (has been assigned a value) and false otherwise */
  public boolean isSetValue() {
    return this.value != null;
  }

  public void setValueIsSet(boolean value) {
    if (!value) {
      this.value = null;
    }
  }

  public boolean isDirty() {
    return this.dirty;
  }

  public CraqObject setDirty(boolean dirty) {
    this.dirty = dirty;
    setDirtyIsSet(true);
    return this;
  }

  public void unsetDirty() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __DIRTY_ISSET_ID);
  }

  /** Returns true if field dirty is set (has been assigned a value) and false otherwise */
  public boolean isSetDirty() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __DIRTY_ISSET_ID);
  }

  public void setDirtyIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __DIRTY_ISSET_ID, value);
  }

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case VALUE:
      if (value == null) {
        unsetValue();
      } else {
        if (value instanceof byte[]) {
          setValue((byte[])value);
        } else {
          setValue((java.nio.ByteBuffer)value);
        }
      }
      break;

    case DIRTY:
      if (value == null) {
        unsetDirty();
      } else {
        setDirty((java.lang.Boolean)value);
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case VALUE:
      return getValue();

    case DIRTY:
      return isDirty();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case VALUE:
      return isSetValue();
    case DIRTY:
      return isSetDirty();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof CraqObject)
      return this.equals((CraqObject)that);
    return false;
  }

  public boolean equals(CraqObject that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_value = true && this.isSetValue();
    boolean that_present_value = true && that.isSetValue();
    if (this_present_value || that_present_value) {
      if (!(this_present_value && that_present_value))
        return false;
      if (!this.value.equals(that.value))
        return false;
    }

    boolean this_present_dirty = true && this.isSetDirty();
    boolean that_present_dirty = true && that.isSetDirty();
    if (this_present_dirty || that_present_dirty) {
      if (!(this_present_dirty && that_present_dirty))
        return false;
      if (this.dirty != that.dirty)
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetValue()) ? 131071 : 524287);
    if (isSetValue())
      hashCode = hashCode * 8191 + value.hashCode();

    hashCode = hashCode * 8191 + ((isSetDirty()) ? 131071 : 524287);
    if (isSetDirty())
      hashCode = hashCode * 8191 + ((dirty) ? 131071 : 524287);

    return hashCode;
  }

  @Override
  public int compareTo(CraqObject other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetValue()).compareTo(other.isSetValue());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetValue()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.value, other.value);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetDirty()).compareTo(other.isSetDirty());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetDirty()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.dirty, other.dirty);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public java.lang.String toString() {
    java.lang.StringBuilder sb = new java.lang.StringBuilder("CraqObject(");
    boolean first = true;

    if (isSetValue()) {
      sb.append("value:");
      if (this.value == null) {
        sb.append("null");
      } else {
        org.apache.thrift.TBaseHelper.toString(this.value, sb);
      }
      first = false;
    }
    if (isSetDirty()) {
      if (!first) sb.append(", ");
      sb.append("dirty:");
      sb.append(this.dirty);
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class CraqObjectStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public CraqObjectStandardScheme getScheme() {
      return new CraqObjectStandardScheme();
    }
  }

  private static class CraqObjectStandardScheme extends org.apache.thrift.scheme.StandardScheme<CraqObject> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, CraqObject struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // VALUE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.value = iprot.readBinary();
              struct.setValueIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // DIRTY
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.dirty = iprot.readBool();
              struct.setDirtyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, CraqObject struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.value != null) {
        if (struct.isSetValue()) {
          oprot.writeFieldBegin(VALUE_FIELD_DESC);
          oprot.writeBinary(struct.value);
          oprot.writeFieldEnd();
        }
      }
      if (struct.isSetDirty()) {
        oprot.writeFieldBegin(DIRTY_FIELD_DESC);
        oprot.writeBool(struct.dirty);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class CraqObjectTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public CraqObjectTupleScheme getScheme() {
      return new CraqObjectTupleScheme();
    }
  }

  private static class CraqObjectTupleScheme extends org.apache.thrift.scheme.TupleScheme<CraqObject> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, CraqObject struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetValue()) {
        optionals.set(0);
      }
      if (struct.isSetDirty()) {
        optionals.set(1);
      }
      oprot.writeBitSet(optionals, 2);
      if (struct.isSetValue()) {
        oprot.writeBinary(struct.value);
      }
      if (struct.isSetDirty()) {
        oprot.writeBool(struct.dirty);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, CraqObject struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      java.util.BitSet incoming = iprot.readBitSet(2);
      if (incoming.get(0)) {
        struct.value = iprot.readBinary();
        struct.setValueIsSet(true);
      }
      if (incoming.get(1)) {
        struct.dirty = iprot.readBool();
        struct.setDirtyIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

