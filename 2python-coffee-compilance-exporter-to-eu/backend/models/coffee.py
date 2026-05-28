from sqlalchemy import Column, Integer, String, Boolean, DateTime, DECIMAL, Enum, ForeignKey, Text, JSON, Table
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from ..db.database import Base

lot_farms = Table(
    "lot_farms",
    Base.metadata,
    Column("lot_id", Integer, ForeignKey("coffee_lots.id", ondelete="CASCADE"), primary_key=True),
    Column("farm_id", Integer, ForeignKey("farms.id", ondelete="CASCADE"), primary_key=True),
)

class Exporter(Base):
    __tablename__ = "exporters"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False)
    email = Column(String(255), nullable=False, unique=True)
    phone = Column(String(50), nullable=True)
    address = Column(Text, nullable=True)
    company_registration = Column(String(100), nullable=True)
    tax_id = Column(String(100), nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String(100), nullable=True)
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)
    
    suppliers = relationship("Supplier", back_populates="exporter")
    coffee_lots = relationship("CoffeeLot", back_populates="exporter")

class Supplier(Base):
    __tablename__ = "suppliers"
    
    id = Column(Integer, primary_key=True, index=True)
    exporter_id = Column(Integer, ForeignKey("exporters.id", ondelete="CASCADE"), nullable=False)
    supplier_type = Column(Enum("farmer", "cooperative", "washing_station", name="supplier_type"), nullable=False)
    name = Column(String(255), nullable=False)
    contact_person = Column(String(100), nullable=True)
    phone = Column(String(50), nullable=True)
    email = Column(String(255), nullable=True)
    gps_coordinates = Column(JSON, nullable=True)
    verification_status = Column(Enum("pending", "verified", "rejected", name="verification_status"), default="pending")
    verification_date = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String(100), nullable=True)
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)
    
    exporter = relationship("Exporter", back_populates="suppliers")
    farms = relationship("Farm", back_populates="supplier")

class Farm(Base):
    __tablename__ = "farms"
    
    id = Column(Integer, primary_key=True, index=True)
    supplier_id = Column(Integer, ForeignKey("suppliers.id", ondelete="CASCADE"), nullable=False)
    farm_name = Column(String(255), nullable=True)
    farm_code = Column(String(100), unique=True, nullable=True)
    boundary_polygon = Column(JSON, nullable=True)
    elevation = Column(String(100), nullable=True)
    variety = Column(String(100), nullable=True)
    harvest_period = Column(String(100), nullable=True)
    size_hectares = Column(DECIMAL(10,2), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String(100), nullable=True)
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)
    
    supplier = relationship("Supplier", back_populates="farms")
    lots = relationship("CoffeeLot", secondary=lot_farms, back_populates="farms")

class CoffeeLot(Base):
    __tablename__ = "coffee_lots"
    
    id = Column(Integer, primary_key=True, index=True)
    exporter_id = Column(Integer, ForeignKey("exporters.id", ondelete="CASCADE"), nullable=False)
    lot_id = Column(String(100), nullable=False)
    name = Column(String(255), nullable=True)
    origin_region = Column(String(100), nullable=True)
    weight_kg = Column(DECIMAL(12,2), nullable=True)
    harvest_start = Column(DateTime, nullable=True)
    harvest_end = Column(DateTime, nullable=True)
    processing_type = Column(String(100), nullable=True)
    grade = Column(String(50), nullable=True)
    status = Column(Enum("registered", "gps_uploaded", "traceability_complete", "ready", "shipped", name="lot_status"), default="registered")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String(100), nullable=True)
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)
    
    exporter = relationship("Exporter", back_populates="coffee_lots")
    shipments = relationship("Shipment", back_populates="coffee_lot")
    farms = relationship("Farm", secondary=lot_farms, back_populates="lots")

class Shipment(Base):
    __tablename__ = "shipments"
    
    id = Column(Integer, primary_key=True, index=True)
    coffee_lot_id = Column(Integer, ForeignKey("coffee_lots.id", ondelete="CASCADE"), nullable=False)
    shipment_code = Column(String(100), nullable=False, unique=True)
    buyer_name = Column(String(255), nullable=True)
    destination_port = Column(String(100), nullable=True)
    departure_date = Column(DateTime, nullable=True)
    arrival_date = Column(DateTime, nullable=True)
    container_number = Column(String(50), nullable=True)
    status = Column(Enum("draft", "ready", "shipped", "delivered", name="shipment_status"), default="draft")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    created_by = Column(String(100), nullable=True)
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)
    
    coffee_lot = relationship("CoffeeLot", back_populates="shipments")

class Document(Base):
    __tablename__ = "documents"
    
    id = Column(Integer, primary_key=True, index=True)
    documentable_type = Column(Enum("farm", "lot", "shipment", "supplier", "exporter", name="documentable_type"), nullable=False)
    documentable_id = Column(Integer, nullable=False)
    document_type = Column(Enum("certificate", "invoice", "legal", "traceability", "cupping", "other", name="document_type"), nullable=False)
    file_path = Column(String(500), nullable=False)
    original_filename = Column(String(255), nullable=True)
    file_size = Column(Integer, nullable=True)
    mime_type = Column(String(100), nullable=True)
    uploaded_by = Column(String(100), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)

class VerificationRecord(Base):
    __tablename__ = "verification_records"
    
    id = Column(Integer, primary_key=True, index=True)
    verifiable_type = Column(String(20), nullable=False)
    verifiable_id = Column(Integer, nullable=False)
    verified_by = Column(String(100), nullable=False)
    verification_date = Column(DateTime(timezone=True), server_default=func.now())
    notes = Column(Text, nullable=True)
    deleted_at = Column(DateTime(timezone=True), nullable=True)

class Buyer(Base):
    __tablename__ = "buyers"
    
    id = Column(Integer, primary_key=True, index=True)
    exporter_id = Column(Integer, ForeignKey("exporters.id", ondelete="CASCADE"), nullable=False)
    company_name = Column(String(255), nullable=False)
    contact_email = Column(String(255), nullable=False)
    access_token = Column(String(255), unique=True, nullable=False, server_default=func.gen_random_uuid())
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)

class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(255), nullable=False, unique=True)
    password_hash = Column(String(255), nullable=False)
    exporter_id = Column(Integer, ForeignKey("exporters.id"), nullable=False)
    role = Column(String(50), default="exporter_admin")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    deleted_at = Column(DateTime(timezone=True), nullable=True)